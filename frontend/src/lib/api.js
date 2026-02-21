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

function resolveFileName(response) {
  const disposition = response.headers.get('content-disposition') || ''
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1].replace(/["']/g, '').trim())
    } catch {
      return utf8Match[1].replace(/["']/g, '').trim()
    }
  }
  const plainMatch = disposition.match(/filename="?([^"]+)"?/i)
  return plainMatch?.[1]?.trim() || null
}

export async function apiDownload(path, { token, method = 'GET', body, headers } = {}) {
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

  if (!response.ok) {
    const raw = await response.text()
    let payload = null
    if (raw) {
      try {
        payload = JSON.parse(raw)
      } catch {
        payload = raw
      }
    }

    const message =
      (payload && typeof payload === 'object' && payload.message) ||
      (typeof payload === 'string' && payload) ||
      `${response.status} ${response.statusText}`
    throw new Error(message)
  }

  return {
    blob: await response.blob(),
    filename: resolveFileName(response),
  }
}
