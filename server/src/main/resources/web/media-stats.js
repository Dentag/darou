const SAMPLE_INTERVAL_MS = 1_500;

export class MediaStats {
  #timer = null;

  start(peer, onQuality, diagnostics) {
    this.stop();
    const previous = new Map();
    let sample = 0;
    const timer = setInterval(async () => {
      try {
        const stats = await peer.getStats();
        if (this.#timer !== timer) return;
        const quality = readQuality(stats, previous);
        if (!quality) return;
        onQuality(quality);
        if (++sample % 4 === 0) diagnostics.record('quality', quality);
      } catch {
        // A peer can close while getStats is in flight.
      }
    }, SAMPLE_INTERVAL_MS);
    this.#timer = timer;
  }

  stop() {
    clearInterval(this.#timer);
    this.#timer = null;
  }
}

function readQuality(stats, previous) {
  let pair;
  stats.forEach(item => {
    if (item.type === 'transport' && item.selectedCandidatePairId) {
      pair = stats.get(item.selectedCandidatePairId);
    }
  });
  if (!pair) {
    stats.forEach(item => {
      if (item.type === 'candidate-pair' && item.nominated && item.state === 'succeeded') pair = item;
    });
  }
  if (!pair) return null;
  const local = stats.get(pair.localCandidateId);
  const remote = stats.get(pair.remoteCandidateId);
  const relay = local?.candidateType === 'relay' || remote?.candidateType === 'relay';
  const rtt = pair.currentRoundTripTime != null ? Math.round(pair.currentRoundTripTime * 1000) : null;
  let received = 0;
  let lost = 0;
  let jitter = 0;
  let measured = false;
  stats.forEach(item => {
    if (item.type !== 'inbound-rtp' || (item.kind !== 'video' && item.mediaType !== 'video')) return;
    const old = previous.get(item.id);
    if (old) {
      received += Math.max(0, item.packetsReceived - old.received);
      lost += Math.max(0, (item.packetsLost || 0) - old.lost);
      measured = true;
    }
    previous.set(item.id, { received: item.packetsReceived, lost: item.packetsLost || 0 });
    jitter = Math.max(jitter, (item.jitter || 0) * 1000);
  });
  const loss = measured && received + lost > 0 ? Math.round(lost / (received + lost) * 1000) / 10 : null;
  return { relay, rtt, loss, jitter: Math.round(jitter) };
}
