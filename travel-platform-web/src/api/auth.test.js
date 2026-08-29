import { describe, expect, it, vi } from 'vitest'

const request = vi.hoisted(() => vi.fn())

vi.mock('@/utils/request', () => ({
  default: request
}))

import { adminLogin, getCurrentAdmin, login, logout, register } from './auth'

describe('认证 API', () => {
  beforeEach(() => {
    request.mockReset()
  })

  it('以 POST 请求提交登录数据', async () => {
    request.mockResolvedValueOnce({ data: { token: 'token-from-api' } })

    const response = await login({ username: 'demo', password: 'secret' })

    expect(response).toEqual({ data: { token: 'token-from-api' } })
    expect(request).toHaveBeenCalledWith({
      url: '/auth/login',
      method: 'post',
      data: { username: 'demo', password: 'secret' }
    })
  })

  it('以 POST 请求提交注册数据', async () => {
    request.mockResolvedValueOnce({ data: { id: 7 } })

    await register({ username: 'new-user', password: 'secret', nickname: '新用户' })

    expect(request).toHaveBeenCalledWith({
      url: '/auth/register',
      method: 'post',
      data: { username: 'new-user', password: 'secret', nickname: '新用户' }
    })
  })

  it('提供管理员登录、当前管理员和退出登录接口', async () => {
    request.mockResolvedValue({ data: { ok: true } })

    await adminLogin({ username: 'admin', password: 'secret' })
    await getCurrentAdmin()
    await logout()

    expect(request.mock.calls).toEqual([
      [{ url: '/admin/auth/login', method: 'post', data: { username: 'admin', password: 'secret' } }],
      [{ url: '/admin/auth/me', method: 'get' }],
      [{ url: '/auth/logout', method: 'post' }]
    ])
  })
})
