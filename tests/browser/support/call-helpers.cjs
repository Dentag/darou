const assert = require('node:assert/strict');
const { until } = require('./wait.cjs');

async function frameCount(page) {
  return page.evaluate(async () => {
    const peer = testPeer();
    if (!peer) return 0;
    let frames = 0;
    for (const stat of (await peer.getStats()).values()) {
      if (stat.type === 'inbound-rtp' && stat.kind === 'video') frames += stat.framesDecoded || 0;
    }
    return frames;
  });
}

async function dial(caller, callee) {
  await caller.click('#call');
  await callee.waitForSelector('#accept:visible');
  await callee.click('#accept');
  await until(async () => await frameCount(caller) > 5 && await frameCount(callee) > 5,
    45_000, 'Both participants must receive decoded video');
  for (const page of [caller, callee]) {
    if (await page.locator('#play').isVisible()) await page.click('#play');
    assert.equal(await page.evaluate(() => document.querySelector('#remote').paused), false);
    const receivingAudio = await page.evaluate(async () => {
      for (const stat of (await testPeer().getStats()).values()) {
        if (stat.type === 'inbound-rtp' && stat.kind === 'audio' && stat.packetsReceived > 0) return true;
      }
      return false;
    });
    assert.equal(receivingAudio, true, 'Participant must receive audio');
  }
}

async function hangup(caller, callee) {
  await caller.click('#hangup');
  await callee.waitForFunction(() => !testBusy());
  assert.equal(await callee.evaluate(() => testStream() === null), true);
}

async function microphoneEnabled(page) {
  return page.evaluate(() => testStream().getAudioTracks()[0].enabled);
}

async function callId(page) {
  return page.evaluate(() => testState.callId);
}

module.exports = { frameCount, dial, hangup, microphoneEnabled, callId };
