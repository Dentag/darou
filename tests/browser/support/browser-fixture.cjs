const installInstrumentation = require('../instrumentation.cjs');
const { createRequire } = require('node:module');
const requireTestDependency = createRequire(require.resolve('../e2e.cjs'));

class BrowserFixture {
  constructor(config) {
    this.config = config;
    this.browser = null;
    this.pageErrors = [];
    this.sessionsReleased = false;
  }

  async start() {
    const { chromium } = requireTestDependency(this.config.playwrightModule);
    this.browser = await chromium.launch({
      executablePath: this.config.chromeExecutable,
      headless: true,
      args: ['--use-fake-device-for-media-stream', '--use-fake-ui-for-media-stream'],
    });
    const options = { permissions: ['camera', 'microphone'], viewport: { width: 390, height: 844 } };
    this.callerContext = await this.browser.newContext(options);
    this.calleeContext = await this.browser.newContext(options);
    this.anonymousContext = await this.browser.newContext(options);
    for (const context of [this.callerContext, this.calleeContext]) {
      await context.addInitScript(installInstrumentation);
    }
    for (const [context, label] of [
      [this.callerContext, 'caller'],
      [this.calleeContext, 'callee'],
      [this.anonymousContext, 'anonymous'],
    ]) {
      context.on('page', page => this.observePage(page, label));
    }
  }

  observePage(page, label) {
    page.on('pageerror', error => {
      this.pageErrors.push(error.message);
      console.error('PAGE ERROR', label, error.message);
    });
    if (!this.config.trace) return;
    page.on('websocket', socket => {
      socket.on('framereceived', event => {
        try {
          const message = JSON.parse(event.payload);
          if (!['ice', 'offer', 'answer'].includes(message.type)) {
            console.log(label, 'received', message.type, message.reason || '');
          }
        } catch {
          // Protocol-level/binary frames are not application messages.
        }
      });
      socket.on('close', () => console.log(label, 'WebSocket closed'));
    });
  }

  async login(context, user) {
    const page = await context.newPage();
    await page.goto(this.config.baseUrl);
    await page.selectOption('#user', user);
    await page.fill('#code', this.config.credentials[user]);
    await page.click('button[type=submit]');
    await page.waitForSelector('#room:visible');
    return page;
  }

  async loginParticipants() {
    this.caller = await this.login(this.callerContext, 'a');
    this.callee = await this.login(this.calleeContext, 'b');
    await this.caller.waitForFunction(() => document.querySelector('#presence').textContent === 'Собеседник в сети');
  }

  async sessionTokens() {
    const cookies = (await this.callerContext.cookies()).concat(await this.calleeContext.cookies());
    return cookies.filter(cookie => cookie.name === 'session').map(cookie => cookie.value);
  }

  async logoutParticipants() {
    if (this.sessionsReleased) return;
    for (const context of [this.callerContext, this.calleeContext]) {
      if (!context) continue;
      const response = await context.request.post(`${this.config.baseUrl}/api/logout`, {
        headers: { Origin: this.config.baseUrl },
        timeout: 5_000,
      });
      if (!response.ok()) throw new Error(`Test logout failed (${response.status()})`);
    }
    this.sessionsReleased = true;
  }

  async printDiagnostics() {
    for (const context of this.browser?.contexts() || []) {
      for (const page of context.pages()) {
        const diagnostic = await page.evaluate(() => ({
          message: document.querySelector('#message')?.textContent,
          peers: window.testDiagnostics,
        })).catch(() => ({}));
        console.error('Diagnostic', JSON.stringify(diagnostic));
      }
    }
  }

  async close() {
    if (!this.browser) return;
    try {
      // Also release real test accounts when a remote scenario fails midway.
      await this.logoutParticipants();
    } finally {
      await this.browser.close();
    }
  }
}

module.exports = { BrowserFixture };
