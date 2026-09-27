const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const { until } = require('../support/wait.cjs');

async function checkServerLogs(server, config, sessionTokens) {
  await until(() => server.logs.includes('event=logout_succeeded'));
  const expectedEvents = [
    'server_started', 'login_rejected', 'login_succeeded', 'signaling_rejected',
    'signaling_connected', 'signaling_closed', 'call_invited', 'call_accepted',
    'call_rejected', 'call_ended',
  ];
  if (config.checks.recovery) expectedEvents.push('signaling_reconnected');
  for (const event of expectedEvents) {
    assert.ok(server.logs.includes(`event=${event}`), `Missing log event: ${event}`);
  }

  const codes = Object.values(config.credentials);
  const sensitiveValues = [
    ...codes,
    ...sessionTokens,
    ...codes.map(code => crypto.createHash('sha256').update(code).digest('hex')),
    'local-test-only', 'a=ice-pwd:', 'a=fingerprint:',
  ];
  for (const value of sensitiveValues) {
    assert.ok(!server.logs.includes(value), 'Sensitive data appeared in server logs');
  }
  assert.ok(!server.logs.includes('event=signaling_failed'), 'Unexpected signaling failure');
  console.log('PASS server lifecycle logs and credential/payload exclusions');
}

module.exports = { checkServerLogs };
