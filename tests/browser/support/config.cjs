const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');

function loadConfig(env = process.env) {
  const serverRoot = path.resolve(__dirname, '../../../server');
  const remote = Boolean(env.TEST_URL);
  const credentials = remote
    ? JSON.parse(fs.readFileSync(env.TEST_ACCESS_FILE, 'utf8'))
    : { a: crypto.randomBytes(16).toString('hex'), b: crypto.randomBytes(16).toString('hex') };

  return {
    serverRoot,
    remote,
    credentials,
    baseUrl: env.TEST_URL || 'http://localhost:8085',
    workDirectory: env.TEST_WORK || path.join(serverRoot, 'build/e2e'),
    javaExecutable: env.JAVA_EXE || 'java',
    chromeExecutable: env.CHROME_EXE,
    playwrightModule: env.PLAYWRIGHT_MODULE || 'playwright',
    trace: Boolean(env.TEST_TRACE),
    checks: {
      invitationTimeout: Boolean(env.TEST_TIMEOUT),
      recovery: Boolean(env.TEST_RECOVERY),
      recoveryTimeout: Boolean(env.TEST_GRACE),
      relayFirst: Boolean(env.TEST_RELAY_FIRST),
    },
  };
}

module.exports = { loadConfig };
