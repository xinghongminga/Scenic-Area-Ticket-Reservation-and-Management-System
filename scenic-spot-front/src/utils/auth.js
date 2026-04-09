function saveAuth(auth) {
  localStorage.setItem('token', auth.token || '');
  localStorage.setItem('role', auth.role || '');
  localStorage.setItem('userId', String(auth.userId || ''));
  localStorage.setItem('nickname', auth.nickname || '');
}

function clearAuth() {
  localStorage.removeItem('token');
  localStorage.removeItem('role');
  localStorage.removeItem('userId');
  localStorage.removeItem('nickname');
}

function getRole() {
  return localStorage.getItem('role') || '';
}

function hasToken() {
  return !!localStorage.getItem('token');
}

export { saveAuth, clearAuth, getRole, hasToken };
