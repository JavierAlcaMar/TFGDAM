const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export function getApiBaseUrl() {
  return API_BASE_URL
}

function buildApiUrl(path) {
  return `${API_BASE_URL}${path}`
}

export function normalizeError(error, fallback) {
  if (!error) return fallback
  if (typeof error === 'string') return error
  return error.message || fallback
}

export async function apiRequest(path, { token, method = 'GET', body, headers } = {}) {
  const requestHeaders = new Headers(headers || {})
  const isFormData = body instanceof FormData

  if (!isFormData && body !== undefined && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json')
  }

  if (token) {
    requestHeaders.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(buildApiUrl(path), {
    method,
    headers: requestHeaders,
    body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
  })

  const raw = await response.text()
  let payload = null
  if (raw) {
    try {
      payload = JSON.parse(raw)
    } catch {
      payload = raw
    }
  }

  if (!response.ok) {
    const message =
      (payload && typeof payload === 'object' && payload.message) ||
      (typeof payload === 'string' && payload) ||
      `${response.status} ${response.statusText}`
    throw new Error(message)
  }

  return payload
}
