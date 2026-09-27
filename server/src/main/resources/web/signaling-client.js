const OPEN_TIMEOUT_MS = 10_000;
const RECONNECT_DELAY_MS = 1_500;
const HEARTBEAT_INTERVAL_MS = 5_000;
const HEARTBEAT_TIMEOUT_MS = 16_000;

export class SignalingClient {
  #socket = null;
  #active = false;
  #ready = false;
  #generation = 0;
  #queue = Promise.resolve();
  #openingTimer;
  #reconnectTimer;
  #heartbeatTimer;
  #lastPong = 0;

  constructor({ onMessage, onClose, onError, onConnecting, onHeartbeatTimeout, diagnostics }) {
    this.onMessage = onMessage;
    this.onClose = onClose;
    this.onError = onError;
    this.onConnecting = onConnecting;
    this.onHeartbeatTimeout = onHeartbeatTimeout;
    this.diagnostics = diagnostics;
    this.instance = sessionStorage.callInstance || (sessionStorage.callInstance = crypto.randomUUID());
  }

  get ready() { return this.#ready; }
  get connecting() { return this.#socket?.readyState === WebSocket.CONNECTING; }

  start() {
    this.#active = true;
    clearInterval(this.#heartbeatTimer);
    this.#heartbeatTimer = setInterval(() => this.heartbeat(), HEARTBEAT_INTERVAL_MS);
    this.reconnect();
  }

  reconnect() {
    if (!this.#active) return;
    clearTimeout(this.#openingTimer);
    clearTimeout(this.#reconnectTimer);
    const generation = ++this.#generation;
    this.#ready = false;
    this.#socket?.close();
    this.onConnecting();
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const socket = new WebSocket(`${protocol}//${location.host}/ws?instance=${encodeURIComponent(this.instance)}`);
    this.#socket = socket;

    this.#openingTimer = setTimeout(() => {
      if (generation === this.#generation && !this.#ready) this.reconnect();
    }, OPEN_TIMEOUT_MS);

    socket.onmessage = event => {
      // SDP operations must finish in order, including across socket replacements.
      this.#queue = this.#queue.then(async () => {
        if (generation !== this.#generation) return;
        const message = JSON.parse(event.data);
        if (message.type === 'pong') {
          this.#lastPong = Date.now();
          return;
        }
        if (message.type === 'ready') {
          this.#ready = true;
          this.#lastPong = Date.now();
          clearTimeout(this.#openingTimer);
          this.diagnostics.record('signaling-ready');
        }
        await this.onMessage(message);
      }).catch(error => {
        if (generation !== this.#generation) return;
        this.onError(error);
      });
    };

    socket.onclose = event => {
      if (generation !== this.#generation) return;
      clearTimeout(this.#openingTimer);
      this.#ready = false;
      this.diagnostics.record('signaling-closed', { code: event.code });
      this.onClose(event);
      if (event.code !== 1008 && this.#active) {
        this.#reconnectTimer = setTimeout(() => this.reconnect(), RECONNECT_DELAY_MS);
      }
    };
  }

  send(message) {
    if (this.#socket?.readyState === WebSocket.OPEN && this.#ready) {
      this.#socket.send(JSON.stringify(message));
    }
  }

  heartbeat() {
    if (!this.#ready) return;
    if (Date.now() - this.#lastPong > HEARTBEAT_TIMEOUT_MS) {
      this.onHeartbeatTimeout();
      this.reconnect();
    } else {
      this.send({ type: 'ping' });
    }
  }

  stop() {
    this.#active = false;
    this.#generation++;
    this.#ready = false;
    clearTimeout(this.#openingTimer);
    clearTimeout(this.#reconnectTimer);
    clearInterval(this.#heartbeatTimer);
    this.#socket?.close();
    this.#socket = null;
  }
}
