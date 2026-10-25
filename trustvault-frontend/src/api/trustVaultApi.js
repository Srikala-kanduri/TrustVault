
export const API = import.meta.env.VITE_API_BASE_URL
  || (import.meta.env.DEV ? 'http://localhost:8080' : '')

export async function request(path, options = {}) {
  const response = await fetch(`${API}${path}`, {
    credentials: 'include',
    ...options,
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Request failed (${response.status})`)
  }

  if (response.status === 204) return null

  const contentType = response.headers.get('content-type') || ''

  return contentType.includes('application/json')
    ? response.json()
    : response.text()
}

export async function csrfToken() {
  return request('/api/csrf')
}

export async function protectedRequest(path, options = {}) {
  const csrf = await csrfToken()

  return request(path, {
    ...options,
    headers: {
      ...(options.headers || {}),
      [csrf.headerName]: csrf.token,
    },
  })
}
