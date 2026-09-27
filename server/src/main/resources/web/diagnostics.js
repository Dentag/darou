const MAX_EVENTS = 240;

export class Diagnostics {
  #events = [];

  record(event, data = {}) {
    this.#events.push({ time: new Date().toISOString(), event, ...data });
    if (this.#events.length > MAX_EVENTS) this.#events.shift();
  }

  download() {
    const blob = new Blob([JSON.stringify({ version: 2, events: this.#events }, null, 2)], {
      type: 'application/json',
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'call-diagnostics.json';
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
  }
}
