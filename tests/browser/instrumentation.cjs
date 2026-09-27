// Installed by Playwright before the page loads. Never shipped as an application asset.
module.exports = function installInstrumentation() {
  const peers = [];
  let currentSocket = null;
  window.testDiagnostics = [];
  window.testState = {
    callId: null,
    revision: 0,
    localRevision: 0,
    remoteRevision: 0,
    signalingReady: false,
    online: false,
  };
  window.testPeer = () => peers.findLast(peer => peer.connectionState !== 'closed') ?? null;
  window.testSocket = () => currentSocket;
  window.testStream = () => document.querySelector('#local')?.srcObject ?? null;
  window.testBusy = () => window.testStream() !== null
    || !document.querySelector('#hangup').hidden || !document.querySelector('#accept').hidden;

  const OriginalPeer = window.RTCPeerConnection;
  window.RTCPeerConnection = class extends OriginalPeer {
    constructor(...args) {
      super(...args);
      peers.push(this);
      const diagnostic = { id: peers.length - 1, states: [], errors: [], candidates: [] };
      window.testDiagnostics.push(diagnostic);
      this.addEventListener('connectionstatechange', () => diagnostic.states.push(this.connectionState));
      this.addEventListener('icecandidateerror', event => {
        diagnostic.errors.push({ code: event.errorCode, text: event.errorText });
      });
      this.addEventListener('icecandidate', event => {
        const candidate = event.candidate;
        if (candidate) {
          diagnostic.candidates.push({
            type: candidate.type,
            protocol: candidate.protocol,
            relayProtocol: candidate.relayProtocol,
          });
        }
      });
    }
  };

  const OriginalSocket = window.WebSocket;
  window.WebSocket = class extends OriginalSocket {
    constructor(...args) {
      super(...args);
      // Anonymous/duplicate-session probes are not the application's connection.
      if (!new URL(args[0], location.href).searchParams.has('instance')) return;
      currentSocket = this;
      window.testState.signalingReady = false;
      this.addEventListener('message', event => {
        if (currentSocket !== this) return;
        const message = JSON.parse(event.data);
        const state = window.testState;
        if (message.type === 'ready') state.signalingReady = true;
        if (message.type === 'presence') state.online = message.online;
        if (['ringing', 'incoming'].includes(message.type)) state.callId = message.callId;
        if (message.callId !== state.callId) return;
        if (message.type === 'negotiate') {
          state.revision = message.revision;
          state.remoteRevision = 0;
        }
        if (['offer', 'answer'].includes(message.type)) state.remoteRevision = message.revision;
        if (message.type === 'ended') {
          state.callId = null;
          state.revision = state.localRevision = state.remoteRevision = 0;
        }
      });
      this.addEventListener('close', () => {
        if (currentSocket !== this) return;
        window.testState.signalingReady = false;
        window.testState.online = false;
      });
    }

    send(data) {
      if (currentSocket === this) {
        const message = JSON.parse(data);
        if (['offer', 'answer'].includes(message.type)) window.testState.localRevision = message.revision;
      }
      return super.send(data);
    }
  };
};
