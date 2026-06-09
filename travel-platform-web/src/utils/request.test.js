import { beforeEach, describe, expect, it, vi } from 'vitest'

const requestUse = vi.fn()
const responseUse = vi.fn()
const fakeService = {
  interceptors: {
    request: { use: requestUse },
    response: { use: responseUse }
  }
}

const create = vi.fn(() => fakeService)
const push = vi.fn()
const clearLogin = vi.fn()

vi.mock('axios', () => ({
  default: { create }
}))

vi.mock('@/router', () => ({
  default: {
    currentRoute: {
      value: {
        fullPath: '/orders/12'
      }
    },
    push
  }
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn()
  }
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    clearLogin
  })
}))

describe('request interceptors', () => {
  beforeEach(() => {
    vi.resetModules()
    requestUse.mockReset()
    responseUse.mockReset()
    push.mockReset()
    clearLogin.mockReset()
    localStorage.clear()
  })

  it('adds bearer token from localStorage to outgoing requests', async () => {
    localStorage.setItem('travel-platform-token', 'abc-token')
    await import('./request')
    const handler = requestUse.mock.calls[0][0]

    const config = handler({ headers: {} })

    expect(config.headers.Authorization).toBe('Bearer abc-token')
  })

  it('clears store auth state and redirects on 401 responses', async () => {
    await import('./request')
    const rejectedHandler = responseUse.mock.calls[0][1]

    await expect(rejectedHandler({ response: { status: 401, data: { message: 'expired' } } }))
      .rejects.toBeTruthy()

    expect(clearLogin).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/orders/12' }
    })
  })
})
