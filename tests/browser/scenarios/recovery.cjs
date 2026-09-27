const assert = require('node:assert/strict');
const fs = require('node:fs');
const { delay, until } = require('../support/wait.cjs');
const { dial, frameCount, microphoneEnabled, callId } = require('../support/call-helpers.cjs');

async function recoverBothParticipants(fixture, originalCall) {
  const { caller, callee, callerContext, calleeContext } = fixture;
  for (const [page, context, label] of [
    [caller, callerContext, 'caller'],
    [callee, calleeContext, 'callee'],
  ]) {
    const before = await caller.evaluate(() => ({
      revision: testState.revision,
      ufrag: testPeer().localDescription.sdp.match(/a=ice-ufrag:([^\r\n]+)/)[1],
    }));
    await context.setOffline(true);
    await page.evaluate(() => testSocket().close());
    await delay(4_000);
    assert.equal(await callId(caller), originalCall);
    assert.equal(await callId(callee), originalCall);
    await context.setOffline(false);
    await page.evaluate(() => window.dispatchEvent(new Event('online')));

    await until(async () => await negotiationComplete(caller, before.revision)
      && await negotiationComplete(callee, before.revision), 30_000, `${label} did not recover`);
    const newUfrag = await caller.evaluate(() => testPeer().localDescription.sdp.match(/a=ice-ufrag:([^\r\n]+)/)[1]);
    assert.notEqual(newUfrag, before.ufrag);
    assert.equal(await callId(caller), originalCall);
    assert.equal(await microphoneEnabled(caller), false);
    const frames = await frameCount(callee);
    await until(async () => await frameCount(callee) > frames + 5, 20_000, 'Fresh video must arrive after recovery');
    console.log(`PASS ${label} signaling outage recovers same call, restarts ICE, preserves mute and receives fresh video`);
  }
}

async function negotiationComplete(page, previousRevision) {
  return page.evaluate(revision => testState.signalingReady
    && testState.revision > revision
    && testState.localRevision === testState.revision
    && testState.remoteRevision === testState.revision
    && testPeer()?.signalingState === 'stable'
    && testPeer()?.connectionState === 'connected', previousRevision);
}

async function checkDiagnosticDownload({ caller, config }) {
  const downloadPromise = caller.waitForEvent('download');
  await caller.click('#diagnostics');
  const download = await downloadPromise;
  const diagnostics = JSON.parse(fs.readFileSync(await download.path(), 'utf8'));
  assert.ok(diagnostics.events.some(event => event.event === 'microphone'));
  assert.ok(diagnostics.events.some(event => event.event === 'negotiate' && event.revision > 1));
  assert.ok(!JSON.stringify(diagnostics).includes(config.credentials.a));
  console.log('PASS diagnostic download');
}

async function checkRecoveryTimeout({ caller, callee, callerContext }) {
  await dial(caller, callee);
  await callerContext.setOffline(true);
  await caller.evaluate(() => testSocket().close());
  await callee.waitForFunction(() => !testBusy(), null, { timeout: 70_000 });
  assert.match(await callee.locator('#message').innerText(), /минуту/);
  assert.equal(await callee.evaluate(() => testStream() === null), true);
  await callerContext.setOffline(false);
  await caller.evaluate(() => window.dispatchEvent(new Event('online')));
  await caller.waitForFunction(() => testState.signalingReady && testState.online);
  console.log('PASS grace period expires, explains reason, releases tracks');
}

module.exports = { recoverBothParticipants, checkDiagnosticDownload, checkRecoveryTimeout };
