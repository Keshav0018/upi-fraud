const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

async function request(path, options) {
  const res = await fetch(`${API_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`
    try {
      const body = await res.json()
      if (body?.message) message = body.message
    } catch {
      // ignore: body wasn't JSON
    }
    throw new Error(message)
  }
  if (res.status === 204) return null
  return res.json()
}

export const api = {
  getStats: () => request('/api/stats'),
  getAlerts: (status) => request(`/api/alerts${status ? `?status=${status}` : ''}`),
  getAlert: (id) => request(`/api/alerts/${id}`),
  decide: (id, decision) =>
    request(`/api/alerts/${id}/decision`, {
      method: 'POST',
      body: JSON.stringify({ decision }),
    }),
  summarize: () => request('/api/analytics/summary', { method: 'POST' }),
}

export const streamUrl = () => `${API_URL}/api/stream`
