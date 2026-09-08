const loginForm = document.querySelector('#login');
const loginButton = document.querySelector('#login-button');
const logoutButton = document.querySelector('#logout');
const refreshButton = document.querySelector('#refresh');
const viewer = document.querySelector('#viewer');
const error = document.querySelector('#error');
const status = document.querySelector('#status');
const log = document.querySelector('#log');
let token = null;
let loading = false;
let timer = null;
let pending = null;

function showError(message = '') {
  error.textContent = message;
  error.hidden = !message;
}

function logout(message = '') {
  token = null;
  clearInterval(timer);
  pending?.abort();
  log.textContent = '';
  viewer.hidden = true;
  logoutButton.hidden = true;
  loginForm.hidden = false;
  document.querySelector('#password').value = '';
  document.querySelector('#username').focus();
  showError(message);
}

async function refresh() {
  if (!token || loading) return;
  loading = true;
  refreshButton.disabled = true;
  const activeToken = token;
  pending = new AbortController();
  const timeout = setTimeout(() => pending?.abort(), 10000);
  try {
    const response = await fetch('/api/notifications', {
      headers: { Authorization: `Bearer ${activeToken}` },
      cache: 'no-store',
      signal: pending.signal
    });
    if (token !== activeToken) return;
    if (response.status === 401) {
      logout('Inloggningen har gått ut. Logga in igen.');
      return;
    }
    if (!response.ok) throw new Error('Kunde inte hämta notifieringarna. Försöker igen automatiskt.');
    const notifications = await response.json();
    if (token !== activeToken) return;
    if (!Array.isArray(notifications)) throw new Error('Tjänsten skickade ett oväntat svar.');
    // Render as text so names and message contents can never become executable HTML.
    log.textContent = notifications.length
      ? notifications.map(item => `[${new Date(item.createdAt).toLocaleString('sv-SE')}] ${item.message}`).join('\n\n')
      : 'Inga notifieringar ännu.';
    status.textContent = `Uppdaterad ${new Date().toLocaleTimeString('sv-SE')}`;
    showError();
  } catch (failure) {
    if (token === activeToken) {
      showError(failure.name === 'AbortError' ? 'Tjänsten svarade inte i tid. Försöker igen automatiskt.' : failure.message);
    }
  } finally {
    clearTimeout(timeout);
    pending = null;
    loading = false;
    refreshButton.disabled = false;
  }
}

loginForm.addEventListener('submit', async event => {
  event.preventDefault();
  showError();
  loginButton.disabled = true;
  loginButton.textContent = 'Loggar in…';
  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      cache: 'no-store',
      body: JSON.stringify({ username: document.querySelector('#username').value, password: document.querySelector('#password').value })
    });
    if (response.status === 401) throw new Error('Fel användarnamn eller lösenord.');
    if (!response.ok) throw new Error('Kunde inte logga in. Kontrollera att kundtjänsten är igång.');
    const result = await response.json();
    if (!result.token) throw new Error('Inloggningen gav ingen token. Försök igen.');
    token = result.token;
    document.querySelector('#password').value = '';
    loginForm.hidden = true;
    viewer.hidden = false;
    logoutButton.hidden = false;
    refreshButton.focus();
    await refresh();
    if (token) timer = setInterval(() => { if (!document.hidden) refresh(); }, 10000);
  } catch (failure) {
    showError(failure.message);
  } finally {
    loginButton.disabled = false;
    loginButton.textContent = 'Logga in';
  }
});

logoutButton.addEventListener('click', () => logout());
refreshButton.addEventListener('click', refresh);
