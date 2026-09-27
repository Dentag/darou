const assert = require('node:assert/strict');
const { loadConfig } = require('./support/config.cjs');
const { LocalServer } = require('./support/local-server.cjs');
const { BrowserFixture } = require('./support/browser-fixture.cjs');
const access = require('./scenarios/access.cjs');
const calls = require('./scenarios/calls.cjs');
const recovery = require('./scenarios/recovery.cjs');
const { checkRelay } = require('./scenarios/relay.cjs');
const { checkServerLogs } = require('./scenarios/logging.cjs');

async function main() {
  const config = loadConfig();
  const server = new LocalServer(config);
  const fixture = new BrowserFixture(config);

  try {
    await server.start();
    await fixture.start();
    await access.checkAnonymousAccess(fixture);
    await fixture.loginParticipants();
    await access.checkDuplicateSession(fixture);
    await calls.checkRejection(fixture);
    if (config.checks.invitationTimeout) await calls.checkInvitationTimeout(fixture);

    // These scenarios intentionally share one active call to verify continuity.
    const originalCall = await calls.startAndCheckMedia(fixture);
    if (config.checks.recovery) {
      await recovery.recoverBothParticipants(fixture, originalCall);
      await recovery.checkDiagnosticDownload(fixture);
    }
    await calls.finishAndCheckCamera(fixture);

    if (config.checks.recoveryTimeout) await recovery.checkRecoveryTimeout(fixture);
    if (config.remote) await checkRelay(fixture);
    await calls.checkDisconnectWhileRinging(fixture);

    const sessionTokens = await fixture.sessionTokens();
    await fixture.logoutParticipants();
    if (!config.remote) await checkServerLogs(server, config, sessionTokens);
    assert.deepEqual(fixture.pageErrors, [], 'Browser pages must not produce unhandled errors');
    console.log('ALL TESTS PASSED');
  } catch (error) {
    console.error(error.stack);
    await fixture.printDiagnostics();
    if (!config.remote) console.error(server.logs.slice(-3000));
    process.exitCode = 1;
  } finally {
    try {
      await fixture.close();
    } finally {
      await server.stop();
    }
  }
}

main().catch(error => {
  console.error(error.stack);
  process.exitCode = 1;
});
