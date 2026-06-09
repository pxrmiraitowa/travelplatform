import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const mocks = vi.hoisted(() => ({
  getCurrentUser: vi.fn(),
  updateCurrentUser: vi.fn(),
  logout: vi.fn()
}))

vi.mock('@/api/user', () => ({
  getCurrentUser: mocks.getCurrentUser,
  updateCurrentUser: mocks.updateCurrentUser
}))

vi.mock('@/api/auth', () => ({
  logout: mocks.logout
}))

import { useUserStore } from './user'

describe('user store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    mocks.getCurrentUser.mockReset()
    mocks.updateCurrentUser.mockReset()
    mocks.logout.mockReset()
  })

  it('persists login state and exposes admin flag', () => {
    const store = useUserStore()

    store.setLogin('token-123', { username: 'admin', roleCodes: ['ROLE_ADMIN'] })

    expect(store.token).toBe('token-123')
    expect(store.isLoggedIn).toBe(true)
    expect(store.isAdmin).toBe(true)
    expect(localStorage.getItem('travel-platform-token')).toBe('token-123')
  })

  it('clears login state from memory and localStorage', () => {
    const store = useUserStore()
    store.setLogin('token-123', { username: 'demo', roleCodes: ['ROLE_USER'] })

    store.clearLogin()

    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('travel-platform-token')).toBeNull()
    expect(localStorage.getItem('travel-platform-user')).toBeNull()
  })

  it('clears local auth state even when logout api fails', async () => {
    const store = useUserStore()
    store.setLogin('token-123', { username: 'demo', roleCodes: ['ROLE_USER'] })
    mocks.logout.mockRejectedValueOnce(new Error('network'))

    await store.logoutAction()

    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('travel-platform-token')).toBeNull()
  })
})
