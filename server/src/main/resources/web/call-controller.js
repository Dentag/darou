import { LocalMedia } from './local-media.js';
import { PeerSession } from './peer-session.js';
import { SignalingClient } from './signaling-client.js';

const RECOVERY_TIMEOUT_MS = 60_000;
const RESTART_INTERVAL_MS = 6_000;
const RECOVERY_CHECK_MS = 5_000;

export class CallController {
  constructor({ api, view, diagnostics }) {
    this.api = api;
    this.view = view;
    this.diagnostics = diagnostics;
    this.user = null;
    this.online = false;
    this.callId = null;
    this.busy = false;
    this.accepted = false;
    this.generation = 0;
    this.recoveringSince = 0;
    this.lastRestart = 0;

    this.media = new LocalMedia(kind => {
      diagnostics.record('track-ended', { kind });
      view.message(kind === 'audio'
        ? 'Браузер остановил микрофон. Перезвони, чтобы включить его снова.'
        : 'Браузер остановил камеру. Перезвони, чтобы включить её снова.');
    });
    this.signaling = new SignalingClient({
      diagnostics,
      onMessage: message => this.receive(message),
      onClose: event => this.signalingClosed(event),
      onError: error => this.signalingError(error),
      onConnecting: () => this.updateControls(),
      onHeartbeatTimeout: () => this.recover('heartbeat-timeout'),
    });
    this.peer = new PeerSession({
      api, diagnostics, media: this.media,
      onSignal: (type, data) => this.signal(type, data),
      onRemoteStream: stream => view.remoteStream(stream),
      onState: state => this.mediaStateChanged(state),
      onQuality: quality => view.quality(quality),
      onInitialTimeout: () => {
        if (!this.recoveringSince) {
          this.signal('end');
          this.reset('Не удалось соединиться за 40 секунд. Попробуй «Только TURN».');
        }
      },
    });
  }

  start() {
    this.view.bind({
      login: (user, code) => this.login(user, code),
      logout: () => this.logout(),
      call: () => this.invite(),
      accept: () => this.accept(),
      reject: () => this.finish('reject', 'Звонок отклонён'),
      hangup: () => this.finish('end', 'Звонок завершён'),
      mic: () => this.toggleTrack('audio'),
      camera: () => this.toggleTrack('video'),
      diagnostics: () => this.diagnostics.download(),
    });
    this.recoveryTimer = setInterval(() => this.checkRecovery(), RECOVERY_CHECK_MS);
    window.addEventListener('online', () => this.networkChanged());
    window.addEventListener('offline', () => this.recover('offline'));
    let networkType = navigator.connection?.type;
    navigator.connection?.addEventListener('change', () => {
      const type = navigator.connection.type;
      if (type && networkType && type !== networkType) this.networkChanged();
      networkType = type;
    });
    window.addEventListener('pagehide', () => {
      this.signal('end');
      this.signaling.stop();
      clearInterval(this.recoveryTimer);
      this.reset();
    });
    window.addEventListener('pageshow', event => {
      if (event.persisted) location.reload();
    });
    this.api.currentUser().then(me => this.enter(me.user)).catch(() => {});
  }

  async login(user, code) {
    try {
      await this.api.login(user, code);
      this.enter(user);
    } catch (error) {
      this.view.message(error.message);
    }
  }

  enter(user) {
    this.user = user;
    this.view.showRoom(user);
    this.signaling.start();
  }

  async logout() {
    try {
      await this.api.logout();
    } catch (error) {
      this.view.message(error.message);
      return;
    }
    this.user = null;
    this.online = false;
    this.signaling.stop();
    this.reset();
    this.view.showLogin();
  }

  async invite() {
    this.busy = true;
    this.updateControls();
    const generation = this.generation;
    this.view.message('Включаем камеру и микрофон…');
    try {
      const stream = await this.media.acquire();
      if (generation !== this.generation) return;
      this.view.localStream(stream);
      this.updateControls();
      if (!this.signaling.ready) throw new Error('Нет связи с сервером');
      this.view.outgoing();
      this.signal('invite');
    } catch (error) {
      if (generation === this.generation) this.reset(humanError(error));
    }
  }

  async accept() {
    const generation = this.generation;
    try {
      const stream = await this.media.acquire();
      if (generation !== this.generation) return;
      this.view.localStream(stream);
      this.updateControls();
      this.signal('accept');
    } catch (error) {
      if (generation === this.generation) this.finish('reject', humanError(error));
    }
  }

  finish(type, text) {
    this.signal(type);
    this.reset(text);
  }

  toggleTrack(kind) {
    const enabled = this.media.toggle(kind);
    this.view.trackEnabled(kind, enabled);
    this.diagnostics.record(kind === 'audio' ? 'microphone' : 'camera', { enabled });
  }

  signal(type, data = {}) {
    this.signaling.send({ type, callId: this.callId, revision: this.peer.revision, ...data });
  }

  async receive(message) {
    const generation = this.generation;
    try {
      await this.handleMessage(message);
    } catch (error) {
      // A WebRTC operation may reject after a hangup; it no longer belongs to this call.
      if (generation === this.generation) this.signalingError(error);
    }
  }

  async handleMessage(message) {
    switch (message.type) {
      case 'ready':
        if (this.callId && message.callId !== this.callId) {
          this.reset('Звонок завершился, пока не было связи с сервером');
        } else if (!this.callId && message.callId) {
          this.signal('end', { callId: message.callId });
        }
        this.updateControls();
        return;
      case 'presence':
        this.online = message.online;
        this.view.presence(this.online, this.accepted);
        this.updateControls();
        return;
      case 'error':
        if (this.callId) this.view.message(message.message);
        else this.reset(message.message);
        return;
      case 'ringing':
        this.callId = message.callId;
        this.view.ringing();
        return;
      case 'incoming':
        this.reset();
        this.callId = message.callId;
        this.busy = true;
        this.view.incoming();
        this.updateControls();
        return;
    }
    if (message.callId !== this.callId) return;
    switch (message.type) {
      case 'ended':
        this.reset(message.reason);
        return;
      case 'reconnecting':
        this.recover('peer-signaling-lost');
        return;
      case 'negotiate':
        if (!this.peer.beginNegotiation(message.revision, this.view.relayOnly)) return;
        this.accepted = true;
        this.lastRestart = Date.now();
        this.view.negotiating(message.revision);
        this.diagnostics.record('negotiate', { revision: message.revision });
        if (message.revision > 1) this.recover('renegotiate');
        if (message.caller === this.user) await this.peer.createOffer();
        return;
    }
    if (message.revision !== this.peer.revision) return;
    switch (message.type) {
      case 'offer': await this.peer.acceptOffer(message.data); break;
      case 'answer': await this.peer.applyAnswer(message.data); break;
      case 'ice': await this.peer.receiveCandidate(message); break;
    }
  }

  signalingClosed(event) {
    this.online = false;
    this.view.disconnected();
    if (event.code === 1008) {
      this.reset(event.reason === 'Already connected'
        ? 'Этот участник уже открыт на другом устройстве или вкладке. Закрой его там и обнови страницу.'
        : 'Время входа истекло. Выйди и войди снова.');
    } else if (this.accepted) {
      this.recover('signaling-lost');
    } else if (this.busy) {
      this.reset('Соединение с сервером прервалось. Попробуй позвонить снова.');
    }
    this.updateControls();
  }

  signalingError(error) {
    this.diagnostics.record('signaling-error', { name: error.name });
    if (this.accepted) this.requestRestart('negotiation-error');
    else this.finish('end', humanError(error));
  }

  mediaStateChanged(state) {
    if (state === 'connected' && this.signaling.ready) {
      this.recoveringSince = 0;
      this.view.message('Вы на связи');
    } else if (['failed', 'disconnected', 'ice-failed'].includes(state)) {
      this.requestRestart(state);
    }
  }

  recover(reason) {
    if (!this.accepted || !this.callId) return;
    if (!this.recoveringSince) {
      this.recoveringSince = Date.now();
      this.diagnostics.record('recovering', { reason });
    }
    this.view.message('Восстанавливаем связь… Звонок сохраняется до минуты.');
  }

  requestRestart(reason) {
    if (!this.accepted || !this.callId) return;
    this.recover(reason);
    if (!this.signaling.ready) {
      if (!this.signaling.connecting) this.signaling.reconnect();
      return;
    }
    if (Date.now() - this.lastRestart < RESTART_INTERVAL_MS) return;
    this.lastRestart = Date.now();
    this.diagnostics.record('ice-restart-request', { reason });
    this.signal('restart');
  }

  checkRecovery() {
    if (!this.user || !this.recoveringSince) return;
    if (Date.now() - this.recoveringSince > RECOVERY_TIMEOUT_MS) {
      this.finish('end', 'Не удалось восстановить связь за минуту. Проверь сеть и перезвони.');
    } else if (this.signaling.ready && this.peer.connected && this.online && this.peer.negotiationComplete) {
      this.recoveringSince = 0;
      this.view.message('Вы на связи');
    } else if (this.signaling.ready && this.online) {
      this.requestRestart('recovery-retry');
    }
  }

  networkChanged() {
    this.diagnostics.record('network-change');
    if (this.accepted) this.recover('network-change');
    if (this.user) this.signaling.reconnect();
  }

  updateControls() {
    this.view.controls({
      online: this.online,
      busy: this.busy,
      signalingReady: this.signaling.ready,
      hasMedia: this.media.stream !== null,
    });
  }

  reset(text = '') {
    this.diagnostics.record('call-ended', { reason: text });
    this.generation++;
    this.peer.close();
    this.media.stop();
    this.callId = null;
    this.busy = false;
    this.accepted = false;
    this.recoveringSince = this.lastRestart = 0;
    this.view.resetCall(text);
    this.updateControls();
  }
}

function humanError(error) {
  switch (error.name) {
    case 'NotAllowedError': return 'Разреши доступ к камере и микрофону в настройках браузера';
    case 'NotFoundError': return 'Камера или микрофон не найдены';
    case 'NotReadableError': return 'Камера занята другим приложением';
    default: return error.message || 'Не удалось начать звонок';
  }
}
