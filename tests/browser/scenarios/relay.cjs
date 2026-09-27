const { dial, hangup } = require('../support/call-helpers.cjs');
const { delay } = require('../support/wait.cjs');

// Requires the explicitly configured remote test deployment and its real TURN service.
async function checkRelay({ caller, callee }) {
  await caller.check('#relay');
  await callee.check('#relay');
  await dial(caller, callee);
  await caller.waitForFunction(() => document.querySelector('#route').textContent.includes('Через TURN'),
    null, { timeout: 15_000 });
  console.log('PASS forced TURN, decoded video both ways:', await caller.locator('#route').innerText());
  await hangup(callee, caller);

  // Limit ICE configuration to TLS relay, independently of successful UDP relay.
  for (const page of [caller, callee]) await page.evaluate(useOnlyTlsRelay);
  await dial(caller, callee);
  await delay(1_800);
  console.log('PASS forced TURN/TLS 443, decoded video both ways:', await caller.locator('#route').innerText());
  await hangup(caller, callee);
}

function useOnlyTlsRelay() {
  const original = window.fetch;
  window.fetch = async (...args) => {
    const response = await original(...args);
    if (args[0] !== '/api/ice') return response;
    const config = await response.json();
    config.iceServers = config.iceServers
      .filter(server => Array.isArray(server.urls))
      .map(server => ({ ...server, urls: server.urls.filter(url => url.startsWith('turns:')) }));
    return new Response(JSON.stringify(config), { headers: { 'Content-Type': 'application/json' } });
  };
}

module.exports = { checkRelay };
