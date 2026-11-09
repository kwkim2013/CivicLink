// 개발 서버에서는 Vite proxy가 /api를 Tomcat(8080)으로 전달하므로
// 브라우저가 다른 출처로 직접 요청하지 않게 한다.
const API_BASE = import.meta.env.VITE_API_BASE_URL || "";
let csrfToken = "";
let csrfRequest;

export class ApiError extends Error {
  constructor(message, status, errorId) {
    super(errorId ? `${message} (오류 ID: ${errorId})` : message);
    this.name = "ApiError";
    this.status = status;
    this.errorId = errorId;
  }
}

export async function prepareCsrf() {
  if (csrfRequest) return csrfRequest;
  csrfRequest = fetchCsrf();
  try {
    return await csrfRequest;
  } finally {
    csrfRequest = null;
  }
}

async function fetchCsrf() {
  const response = await fetch(`${API_BASE}/api/auth/csrf`, {
    credentials: "include",
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok || typeof data.csrfToken !== "string" || !data.csrfToken)
    throw new ApiError(
      data.message || "CSRF 토큰 발급에 실패했습니다.",
      response.status,
    );
  csrfToken = data.csrfToken;
}

async function request(path, options = {}) {
  const headers = {
    Accept: "application/json",
    ...(options.body && { "Content-Type": "application/json" }),
    ...(options.headers || {}),
  };
  if (csrfToken && options.method && options.method !== "GET")
    headers["X-CSRF-Token"] = csrfToken;
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    credentials: "include",
    headers,
  });
  let data = null;
  try {
    data = await response.json();
  } catch {
    /* empty response */
  }
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith("/api/auth/"))
      window.dispatchEvent(new Event("auth-expired"));
    throw new ApiError(
      data?.message || data?.error || `요청 실패 (${response.status})`,
      response.status,
      data?.errorId,
    );
  }
  return data;
}

export async function api(path, options = {}) {
  options = { ...options, method: (options.method || "GET").toUpperCase() };
  if (!["GET", "HEAD", "OPTIONS"].includes(options.method) && !csrfToken)
    await prepareCsrf();
  try {
    return await request(path, options);
  } catch (error) {
    // Tomcat 재시작이나 세션 만료로 CSRF 토큰이 달라진 경우 한 번만 재발급 후 재시도한다.
    if (error.status === 403 && error.message.includes("CSRF")) {
      csrfToken = "";
      await prepareCsrf();
      return request(path, options);
    }
    throw error;
  }
}

export async function login(username, password) {
  const data = await api("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  csrfToken = data.csrfToken;
  return data.user;
}

export async function register(username, password) {
  return api("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}
export async function logout() {
  try {
    return await api("/api/auth/logout", { method: "POST" });
  } catch (error) {
    if (error.status !== 401) throw error;
  } finally {
    csrfToken = "";
  }
}
