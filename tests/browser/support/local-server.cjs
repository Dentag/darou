const { spawn } = require('node:child_process');
const { once } = require('node:events');
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const { until } = require('./wait.cjs');

class LocalServer {
  constructor(config) {
    this.config = config;
    this.process = null;
    this.logs = '';
    this.startError = null;
  }

  async start() {
    const config = this.config;
    fs.mkdirSync(config.workDirectory, { recursive: true });
    if (!config.remote) {
      const directory = path.join(config.workDirectory, 'test-credentials');
      fs.mkdirSync(directory, { recursive: true });
      fs.writeFileSync(path.join(directory, 'turn-secret'), 'local-test-only');
      this.process = spawn(config.javaExecutable, [
        '-cp', path.join(config.serverRoot, 'build/install/server/lib/*'),
        'dev.dentag.darou.server.MainKt',
      ], {
        env: {
          ...process.env,
          PUBLIC_ORIGIN: config.baseUrl,
          PORT: '8085',
          TURN_HOST: 'turn.example.invalid',
          CREDENTIALS_DIRECTORY: directory,
          LOGIN_A_SHA256: hash(config.credentials.a),
          LOGIN_B_SHA256: hash(config.credentials.b),
        },
        windowsHide: true,
      });
      this.process.stdout.on('data', data => { this.logs += data; });
      this.process.stderr.on('data', data => { this.logs += data; });
      this.process.on('error', error => { this.startError = error; });
    }

    await until(async () => {
      if (this.startError || this.process?.exitCode != null) return true;
      return (await fetch(`${config.baseUrl}/health`)).ok;
    }, 25_000, 'Server did not become available');
    if (this.startError) throw this.startError;
    if (this.process?.exitCode != null) throw new Error('Local server exited during startup');
  }

  async stop() {
    const child = this.process;
    if (!child?.pid || child.exitCode != null || child.signalCode != null) return;
    const exited = once(child, 'exit');
    child.kill();
    const forceStop = setTimeout(() => child.kill('SIGKILL'), 5_000);
    try {
      await exited;
    } finally {
      clearTimeout(forceStop);
    }
  }
}

function hash(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

module.exports = { LocalServer };
