import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from './index'
import { useUserStore } from '@/stores/user'

describe('路由守卫权限', () => {
  beforeEach(async () => {
    localStorage.clear()
    setActivePinia(createPinia())
    window.scrollTo = vi.fn()
    await router.push('/login')
  })

  it('未登录访问用户订单时跳转登录并保留原路径', async () => {
    await router.push('/orders')

    expect(router.currentRoute.value.fullPath).toBe('/login?redirect=/orders')
  })

  it('未登录访问后台时跳转后台登录并保留原路径', async () => {
    await router.push('/admin/users')

    expect(router.currentRoute.value.fullPath).toBe('/admin/login?redirect=/admin/users')
  })

  it('普通用户访问后台时返回首页', async () => {
    const userStore = useUserStore()
    userStore.setLogin('user-token', { username: 'demo', roleCodes: ['ROLE_USER'] })

    await router.push('/admin/dashboard')

    expect(router.currentRoute.value.fullPath).toBe('/')
  })
})
