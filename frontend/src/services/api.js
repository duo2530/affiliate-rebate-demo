async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
  })
  const result = await response.json().catch(() => ({ code: 'HTTP_ERROR', message: '服务暂不可用', data: null }))
  if (!response.ok || result.code !== 'OK') {
    const error = new Error(result.message || '请求失败')
    error.code = result.code
    throw error
  }
  return result
}
const page = (extra = '') => '?page=1&pageSize=20' + extra
export const api = {
  me: () => request('/api/v1/auth/me'),
  login: (body) => request('/api/v1/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  register: (body) => request('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  logout: () => request('/api/v1/auth/logout', { method: 'POST' }),
  overview: () => request('/api/v1/users/me/affiliate'),
  invitees: () => request('/api/v1/users/me/invitees' + page()),
  rebates: () => request('/api/v1/users/me/rebates' + page()),
  recharges: () => request('/api/v1/users/me/recharges' + page()),
  simulateRecharge: (amount, key) => request('/api/v1/recharges/simulate', {
    method: 'POST', headers: { 'Idempotency-Key': key }, body: JSON.stringify({ amount }),
  }),
  admin: {
    users: () => request('/api/v1/admin/users' + page()),
    relations: () => request('/api/v1/admin/referral-relations' + page()),
    recharges: (keyword = '') => request('/api/v1/admin/recharge-orders' + page(keyword ? '&keyword=' + encodeURIComponent(keyword) : '')),
    rebates: () => request('/api/v1/admin/rebate-ledgers' + page()),
  },
}
