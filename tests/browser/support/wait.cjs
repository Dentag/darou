const delay = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));

async function until(predicate, timeoutMs = 20_000, description = 'Condition timed out') {
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeoutMs) {
    try {
      if (await predicate()) return;
    } catch {
      // The page or server can temporarily be unavailable during startup/recovery.
    }
    await delay(200);
  }
  throw new Error(description);
}

module.exports = { delay, until };
