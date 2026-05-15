const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://39.96.52.107:8080';

function handleUnauthorized() {
  localStorage.removeItem('token');
  localStorage.removeItem('role');
  localStorage.removeItem('userId');
  localStorage.removeItem('nickname');
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

function getToken() {
  return localStorage.getItem('token') || '';
}

async function request(path, options = {}) {
  const headers = {
    ...(options.headers || {})
  };
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const config = {
    method: options.method || 'GET',
    headers
  };

  if (options.body instanceof FormData) {
    config.body = options.body;
  } else if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    config.body = JSON.stringify(options.body);
  }

  const controller = new AbortController();
  const timeoutMs = Number(options.timeoutMs || 0);
  let timer = null;
  if (timeoutMs > 0) {
    timer = setTimeout(() => controller.abort(), timeoutMs);
  }

  config.signal = controller.signal;

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, config);
  } catch (err) {
    if (err?.name === 'AbortError') {
      throw new Error(`请求超时（>${timeoutMs}ms）`);
    }
    throw err;
  } finally {
    if (timer) {
      clearTimeout(timer);
    }
  }
  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }

  if (!response.ok) {
    if (response.status === 401) {
      handleUnauthorized();
    }
    const msg = data?.message || `请求失败(${response.status})`;
    throw new Error(msg);
  }

  if (data && data.success === false) {
    throw new Error(data.message || '业务异常');
  }

  return data?.data;
}

async function download(path) {
  const headers = {};
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const response = await fetch(`${BASE_URL}${path}`, { headers });
  if (!response.ok) {
    if (response.status === 401) {
      handleUnauthorized();
    }
    throw new Error(`下载失败(${response.status})`);
  }
  return response.blob();
}

export { request, download, BASE_URL };
