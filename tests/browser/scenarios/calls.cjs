const assert = require('node:assert/strict');
const path = require('node:path');
const { delay } = require('../support/wait.cjs');
const { dial, hangup, frameCount, microphoneEnabled, callId } = require('../support/call-helpers.cjs');

async function checkRejection({ caller, callee }) {
  await caller.click('#call');
  await callee.waitForSelector('#reject:visible');
  await callee.click('#reject');
  await caller.waitForFunction(() => !testBusy());
  assert.equal(await caller.evaluate(() => testStream() === null), true);
  console.log('PASS reject stops camera');
}

async function checkInvitationTimeout({ caller, callee }) {
  await caller.click('#call');
  await callee.waitForSelector('#accept:visible');
  await caller.waitForFunction(() => !testBusy(), null, { timeout: 55_000 });
  assert.equal(await caller.evaluate(() => testStream() === null), true);
  assert.match(await caller.locator('#message').innerText(), /Нет ответа/);
  console.log('PASS unanswered invitation expires and stops camera');
}

// Leaves an active call with the caller's microphone muted for the recovery scenarios.
async function startAndCheckMedia({ caller, callee, config }) {
  if (config.checks.relayFirst) {
    await caller.check('#relay');
    await callee.check('#relay');
  }
  await dial(caller, callee);
  await callee.evaluate(() => testSocket().send(JSON.stringify({ type: 'end', callId: 'forged-id' })));
  await delay(300);
  assert.equal(await caller.evaluate(() => testPeer().connectionState), 'connected');

  await caller.click('#mic');
  assert.equal(await microphoneEnabled(caller), false);
  const originalCall = await callId(caller);
  const mutedFrames = await frameCount(callee);
  await delay(7_000);
  assert.equal(await callId(caller), originalCall);
  assert.equal(await caller.evaluate(() => testPeer().connectionState), 'connected');
  assert.ok(await frameCount(callee) > mutedFrames, 'Video must continue while microphone is muted');
  for (let index = 0; index < 4; index++) await caller.click('#mic');
  assert.equal(await microphoneEnabled(caller), false);
  console.log('PASS muted microphone preserves active call and video; repeated toggles');
  return originalCall;
}

async function finishAndCheckCamera({ caller, callee, config }) {
  await caller.click('#camera');
  assert.equal(await caller.evaluate(() => testStream().getVideoTracks()[0].enabled), false);
  await delay(1_600);
  console.log('PASS bidirectional decoded video, mute, camera toggle, invalid call ID; route:',
    await caller.locator('#route').innerText());
  await caller.screenshot({ path: path.join(config.workDirectory, 'call-mobile.png'), fullPage: true });
  await hangup(caller, callee);
}

async function checkDisconnectWhileRinging({ caller, callee }) {
  await caller.click('#call');
  await callee.waitForSelector('#accept:visible');
  await callee.close();
  await caller.waitForFunction(() => !testBusy());
  assert.equal(await caller.evaluate(() => testStream() === null), true);
  console.log('PASS disconnect ends call and stops tracks');
}

module.exports = {
  checkRejection,
  checkInvitationTimeout,
  startAndCheckMedia,
  finishAndCheckCamera,
  checkDisconnectWhileRinging,
};
