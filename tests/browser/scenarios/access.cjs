const assert = require('node:assert/strict');

async function checkAnonymousAccess(fixture) {
  const { anonymousContext: context, config } = fixture;
  const ice = await context.request.get(`${config.baseUrl}/api/ice`);
  assert.equal(ice.status(), 401);

  const wrongOrigin = await context.request.post(`${config.baseUrl}/api/login`, {
    headers: { Origin: 'https://untrusted.invalid' },
    data: { user: 'a', code: config.credentials.a },
  });
  assert.equal(wrongOrigin.status(), 403);

  const wrongCode = await context.request.post(`${config.baseUrl}/api/login`, {
    headers: { Origin: config.baseUrl },
    data: { user: 'a', code: 'wrong' },
  });
  assert.equal(wrongCode.status(), 401);

  const page = await context.newPage();
  await page.goto(config.baseUrl);
  const closeCode = await page.evaluate(() => new Promise(resolve => {
    const socket = new WebSocket(location.origin.replace('http', 'ws') + '/ws');
    socket.onclose = event => resolve(event.code);
  }));
  assert.equal(closeCode, 1008);
  await page.close();
  console.log('PASS unauthorized ICE, wrong code, wrong origin, anonymous WebSocket');
}

async function checkDuplicateSession(fixture) {
  const reason = await fixture.caller.evaluate(() => new Promise(resolve => {
    const socket = new WebSocket(location.origin.replace('http', 'ws') + '/ws');
    socket.onclose = event => resolve(event.reason);
  }));
  assert.equal(reason, 'Already connected');
  console.log('PASS duplicate session blocked');
}

module.exports = { checkAnonymousAccess, checkDuplicateSession };
