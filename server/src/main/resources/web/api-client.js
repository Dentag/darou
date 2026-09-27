export class ApiClient {
  async request(path, options = {}) {
    const response = await fetch(path, { cache: 'no-store', ...options });
    if (!response.ok) {
      const message = response.status === 401
        ? 'Проверь код доступа или войди заново'
        : response.status === 429
          ? 'Слишком много попыток. Попробуй через 10 минут'
          : `Сервер недоступен (${response.status})`;
      throw new Error(message);
    }
    return response.json();
  }

  login(user, code) {
    return this.request('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ user, code }),
    });
  }

  currentUser() { return this.request('/api/me'); }
  logout() { return this.request('/api/logout', { method: 'POST' }); }
  iceConfiguration() { return this.request('/api/ice'); }
}
