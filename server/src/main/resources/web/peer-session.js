import { MediaStats } from './media-stats.js';

const ICE_REFRESH_MS = 45 * 60 * 1000;
const CONNECTION_TIMEOUT_MS = 40_000;
const MAX_PENDING_CANDIDATES = 128;

export class PeerSession {
  #peer = null;
  #generation = 0;
  #revision = 0;
  #localRevision = 0;
  #remoteRevision = 0;
  #pending = [];
  #iceLoadedAt = 0;
  #connectionTimer;
  #wasConnected = false;
  #stats = new MediaStats();

  constructor({ api, media, diagnostics, onSignal, onRemoteStream, onState, onInitialTimeout, onQuality }) {
    this.api = api;
    this.media = media;
    this.diagnostics = diagnostics;
    this.onSignal = onSignal;
    this.onRemoteStream = onRemoteStream;
    this.onState = onState;
    this.onInitialTimeout = onInitialTimeout;
    this.onQuality = onQuality;
    this.relayOnly = false;
  }

  get revision() { return this.#revision; }
  get connected() { return this.#peer?.connectionState === 'connected'; }
  get negotiationComplete() {
    return this.#revision > 0 && this.#localRevision === this.#revision && this.#remoteRevision === this.#revision;
  }

  beginNegotiation(revision, relayOnly) {
    if (revision <= this.#revision) return false;
    this.#revision = revision;
    this.#remoteRevision = 0;
    this.#pending = [];
    this.relayOnly = relayOnly;
    return true;
  }

  async ensurePeer() {
    if (this.#peer) return this.#peer;
    const generation = this.#generation;
    const config = await this.api.iceConfiguration();
    if (generation !== this.#generation) return null;
    const peer = new RTCPeerConnection({
      ...config,
      bundlePolicy: 'max-bundle',
      iceTransportPolicy: this.relayOnly ? 'relay' : 'all',
    });
    this.#peer = peer;
    this.#iceLoadedAt = Date.now();
    this.media.stream.getTracks().forEach(track => peer.addTrack(track, this.media.stream));
    peer.onicecandidate = event => {
      if (!event.candidate || this.#peer !== peer || !this.#localRevision) return;
      const ufrag = iceUfrag(peer.localDescription);
      if (event.candidate.usernameFragment && ufrag && event.candidate.usernameFragment !== ufrag) return;
      this.onSignal('ice', { revision: this.#localRevision, data: event.candidate.toJSON() });
    };
    peer.onicecandidateerror = event => {
      if (this.#peer === peer) this.diagnostics.record('ice-error', { code: event.errorCode });
    };
    peer.ontrack = event => {
      if (this.#peer === peer) this.onRemoteStream(event.streams[0]);
    };
    peer.onconnectionstatechange = () => {
      if (this.#peer !== peer) return;
      const state = peer.connectionState;
      this.diagnostics.record('media-state', { state });
      if (state === 'connected') {
        this.#wasConnected = true;
        clearTimeout(this.#connectionTimer);
      }
      this.onState(state);
    };
    peer.oniceconnectionstatechange = () => {
      if (this.#peer === peer && peer.iceConnectionState === 'failed') this.onState('ice-failed');
    };
    this.#connectionTimer = setTimeout(() => {
      if (this.#peer === peer && !this.#wasConnected) this.onInitialTimeout();
    }, CONNECTION_TIMEOUT_MS);
    this.#stats.start(peer, this.onQuality, this.diagnostics);
    return peer;
  }

  async refreshIce(peer) {
    if (Date.now() - this.#iceLoadedAt < ICE_REFRESH_MS) return;
    const config = await this.api.iceConfiguration();
    if (this.#peer !== peer) return;
    peer.setConfiguration({ ...peer.getConfiguration(), iceServers: config.iceServers });
    this.#iceLoadedAt = Date.now();
  }

  async createOffer() {
    const peer = await this.ensurePeer();
    if (!peer || this.#peer !== peer) return;
    if (peer.signalingState !== 'stable') await peer.setLocalDescription({ type: 'rollback' });
    await this.refreshIce(peer);
    if (this.#peer !== peer) return;
    const offer = await peer.createOffer({ iceRestart: this.#revision > 1 });
    if (this.#peer !== peer) return;
    await peer.setLocalDescription(offer);
    if (this.#peer !== peer) return;
    this.#localRevision = this.#revision;
    this.onSignal('offer', { data: peer.localDescription.toJSON() });
  }

  async acceptOffer(data) {
    const peer = await this.ensurePeer();
    if (!peer || this.#peer !== peer) return;
    await this.refreshIce(peer);
    if (this.#peer !== peer || !await this.setRemoteDescription(peer, data)) return;
    const answer = await peer.createAnswer();
    if (this.#peer !== peer) return;
    await peer.setLocalDescription(answer);
    if (this.#peer !== peer) return;
    this.#localRevision = this.#revision;
    this.onSignal('answer', { data: peer.localDescription.toJSON() });
  }

  async applyAnswer(data) {
    const peer = await this.ensurePeer();
    if (peer && this.#peer === peer) await this.setRemoteDescription(peer, data);
  }

  async setRemoteDescription(peer, data) {
    await peer.setRemoteDescription(data);
    if (this.#peer !== peer) return false;
    this.#remoteRevision = this.#revision;
    const pending = this.#pending;
    this.#pending = [];
    for (const item of pending) {
      if (this.#peer !== peer) return false;
      if (item.revision === this.#revision) await this.addCandidate(peer, item.data);
    }
    return this.#peer === peer;
  }

  async receiveCandidate(message) {
    if (this.#peer?.remoteDescription && this.#remoteRevision === this.#revision) {
      await this.addCandidate(this.#peer, message.data);
    } else if (this.#pending.length < MAX_PENDING_CANDIDATES) {
      this.#pending.push(message);
    }
  }

  async addCandidate(peer, data) {
    const ufrag = iceUfrag(peer.remoteDescription);
    if (data.usernameFragment && ufrag && data.usernameFragment !== ufrag) return;
    try {
      await peer.addIceCandidate(data);
    } catch (error) {
      if (this.#peer === peer) this.diagnostics.record('candidate-rejected', { name: error.name });
    }
  }

  close() {
    this.#generation++;
    this.#stats.stop();
    clearTimeout(this.#connectionTimer);
    const peer = this.#peer;
    this.#peer = null;
    peer?.close();
    this.#revision = this.#localRevision = this.#remoteRevision = 0;
    this.#pending = [];
    this.#iceLoadedAt = 0;
    this.#wasConnected = false;
  }
}

function iceUfrag(description) {
  return description?.sdp.match(/a=ice-ufrag:([^\r\n]+)/)?.[1];
}
