import { expect } from 'vitest'

export const API_BASE = (process.env.API_BASE_URL || 'http://localhost:8000/api').replace(/\/$/, '')

export const DIRECT_SERVICE_BASES = {
  gateway: 'http://localhost:8000/api',
  user: 'http://localhost:8101/api',
  product: 'http://localhost:8102/api',
  order: 'http://localhost:8103/api',
  contentTrip: 'http://localhost:8104/api'
}

export function uniqueSuffix() {
  return `${Date.now()}${Math.floor(Math.random() * 10000)}`
}

export async function api(path, options = {}) {
  const {
    base = API_BASE,
    method = 'GET',
    token,
    body,
    formData,
    expectedStatus = 200
  } = options

  const headers = {}
  const init = { method, headers }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (formData) {
    init.body = formData
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(body)
  }

  const response = await fetch(`${base}${path}`, init)
  const text = await response.text()
  let payload = null
  if (text) {
    try {
      payload = JSON.parse(text)
    } catch (error) {
      throw new Error(`${method} ${path} returned non-JSON response: ${text}`)
    }
  }

  expect(response.status, `${method} ${path} HTTP status`).toBe(expectedStatus)
  if (payload && Object.prototype.hasOwnProperty.call(payload, 'code')) {
    expect(payload.code, `${method} ${path} business code`).toBe(200)
  }
  return payload
}

export async function login(username = 'demo_user', password = '123456') {
  const result = await api('/auth/login', {
    method: 'POST',
    body: { username, password }
  })
  expect(result.data.token).toBeTruthy()
  return result.data
}

export async function adminLogin() {
  return login('admin', '123456')
}

export function records(result) {
  const data = result.data
  if (Array.isArray(data)) {
    return data
  }
  return data?.records || []
}

export function firstRecord(result, label) {
  const rows = records(result)
  expect(rows.length, `${label} should have demo data`).toBeGreaterThan(0)
  return rows[0]
}

export async function waitForGateway() {
  const startedAt = Date.now()
  let lastError
  while (Date.now() - startedAt < 60_000) {
    try {
      const result = await api('/public/health')
      if (result.data?.status === 'UP') {
        return
      }
    } catch (error) {
      lastError = error
    }
    await new Promise(resolve => setTimeout(resolve, 1500))
  }
  throw lastError || new Error('gateway did not become healthy within 60s')
}

export async function uploadTinyPng(path, token) {
  const pngBytes = Uint8Array.from([
    0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
    0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
    0x08, 0x06, 0x00, 0x00, 0x00, 0x1f, 0x15, 0xc4,
    0x89, 0x00, 0x00, 0x00, 0x0d, 0x49, 0x44, 0x41,
    0x54, 0x78, 0x9c, 0x63, 0xf8, 0xff, 0xff, 0x3f,
    0x00, 0x05, 0xfe, 0x02, 0xfe, 0xdc, 0xcc, 0x59,
    0xe7, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4e,
    0x44, 0xae, 0x42, 0x60, 0x82
  ])
  const form = new FormData()
  form.append('file', new Blob([pngBytes], { type: 'image/png' }), 'regression.png')
  const result = await api(path, { method: 'POST', token, formData: form })
  expect(result.data.url).toMatch(/^\/api\/public\//)
  return result.data.url
}

export async function createOrder(token, overrides = {}) {
  const body = {
    productType: 'FLIGHT',
    productId: 1,
    quantity: 1,
    travelDate: '2030-07-02',
    contactName: '回归测试',
    contactPhone: '13800000000',
    ...overrides
  }
  const result = await api('/orders', { method: 'POST', token, body })
  expect(result.data.id).toBeTruthy()
  return result.data
}
