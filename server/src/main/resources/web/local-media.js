export class LocalMedia {
  #stream = null;
  #generation = 0;

  constructor(onTrackEnded) {
    this.onTrackEnded = onTrackEnded;
  }

  get stream() { return this.#stream; }

  async acquire() {
    if (this.#stream) return this.#stream;
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new Error('Открой ссылку в Safari или Chrome по HTTPS');
    }
    const generation = this.#generation;
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: {
        width: { ideal: 640 },
        height: { ideal: 480 },
        frameRate: { ideal: 24, max: 30 },
        facingMode: 'user',
      },
    });
    // The permission dialog can finish after a hangup or logout.
    if (generation !== this.#generation) {
      stream.getTracks().forEach(track => track.stop());
      throw new Error('Звонок уже завершён');
    }
    this.#stream = stream;
    for (const track of stream.getTracks()) {
      track.addEventListener('ended', () => {
        if (this.#stream === stream) this.onTrackEnded(track.kind);
      });
    }
    return stream;
  }

  toggle(kind) {
    const tracks = this.#stream?.getTracks().filter(track => track.kind === kind) ?? [];
    const enabled = !tracks.some(track => track.enabled);
    tracks.forEach(track => { track.enabled = enabled; });
    return enabled;
  }

  stop() {
    this.#generation++;
    this.#stream?.getTracks().forEach(track => track.stop());
    this.#stream = null;
  }
}
