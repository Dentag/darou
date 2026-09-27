export class CallView {
  constructor() {
    const ids = [
      'login', 'room', 'loginForm', 'user', 'code', 'identity', 'presence', 'dot',
      'local', 'remote', 'placeholder', 'play', 'route', 'quality', 'message',
      'call', 'accept', 'reject', 'hangup', 'mic', 'camera', 'relay', 'logout', 'diagnostics',
    ];
    this.elements = Object.fromEntries(ids.map(id => [id, document.getElementById(id)]));
  }

  bind(actions) {
    const elements = this.elements;
    elements.loginForm.onsubmit = async event => {
      event.preventDefault();
      const button = event.submitter;
      button.disabled = true;
      try {
        await actions.login(elements.user.value, elements.code.value);
      } finally {
        button.disabled = false;
      }
    };
    elements.accept.onclick = async () => {
      elements.accept.disabled = true;
      try {
        await actions.accept();
      } finally {
        elements.accept.disabled = false;
      }
    };
    for (const id of ['call', 'reject', 'hangup', 'mic', 'camera', 'logout', 'diagnostics']) {
      elements[id].onclick = () => actions[id]();
    }
    elements.play.onclick = () => {
      elements.remote.play()
        .then(() => { elements.play.hidden = true; })
        .catch(() => this.message('Браузер не разрешил воспроизведение'));
    };
  }

  get relayOnly() { return this.elements.relay.checked; }
  message(text = '') { this.elements.message.textContent = text; }

  showRoom(user) {
    this.elements.login.hidden = true;
    this.elements.room.hidden = false;
    this.elements.identity.textContent = `УЧАСТНИК ${user === 'a' ? '1' : '2'}`;
    this.elements.code.value = '';
  }

  showLogin() {
    this.elements.room.hidden = true;
    this.elements.login.hidden = false;
  }

  controls({ online, busy, signalingReady, hasMedia }) {
    this.elements.call.disabled = !online || busy || !signalingReady;
    this.elements.relay.disabled = busy;
    this.elements.mic.disabled = !hasMedia;
    this.elements.camera.disabled = !hasMedia;
  }

  presence(online, accepted) {
    this.elements.presence.textContent = online
      ? 'Собеседник в сети'
      : accepted ? 'Собеседник восстанавливает связь' : 'Ждём второго участника';
    this.elements.dot.classList.toggle('online', online);
  }

  disconnected() {
    this.elements.dot.classList.remove('online');
    this.elements.presence.textContent = 'Нет связи с сервером';
  }

  localStream(stream) { this.elements.local.srcObject = stream; }

  remoteStream(stream) {
    this.elements.remote.srcObject = stream;
    this.elements.placeholder.hidden = true;
    this.elements.remote.play().catch(() => {
      if (this.elements.remote.srcObject === stream) this.elements.play.hidden = false;
    });
  }

  outgoing() { this.elements.call.hidden = true; }

  ringing() {
    this.elements.hangup.hidden = false;
    this.message('Звоним…');
  }

  incoming() {
    this.elements.call.hidden = true;
    this.elements.accept.hidden = false;
    this.elements.reject.hidden = false;
    this.message('Входящий звонок');
  }

  negotiating(revision) {
    this.elements.accept.hidden = true;
    this.elements.reject.hidden = true;
    this.elements.hangup.hidden = false;
    this.message(revision === 1 ? 'Устанавливаем соединение…' : 'Восстанавливаем соединение…');
  }

  trackEnabled(kind, enabled) {
    const id = kind === 'audio' ? 'mic' : 'camera';
    const label = kind === 'audio' ? 'Микрофон' : 'Камера';
    this.elements[id].textContent = `${label}: ${enabled ? 'вкл.' : 'выкл.'}`;
  }

  quality({ relay, rtt, loss, jitter }) {
    this.elements.route.textContent = (relay ? 'Через TURN' : 'Напрямую')
      + (rtt !== null ? ` · ${rtt} мс` : '');
    this.elements.quality.textContent = loss === null
      ? '' : `Приём видео: потери ${loss}% · джиттер ${jitter} мс`;
  }

  resetCall(text = '') {
    this.elements.local.srcObject = null;
    this.elements.remote.srcObject = null;
    this.elements.placeholder.hidden = false;
    this.elements.play.hidden = true;
    for (const id of ['accept', 'reject', 'hangup']) this.elements[id].hidden = true;
    this.elements.call.hidden = false;
    this.elements.route.textContent = 'Ожидание звонка';
    this.elements.quality.textContent = '';
    this.trackEnabled('audio', true);
    this.trackEnabled('video', true);
    this.message(text);
  }
}
