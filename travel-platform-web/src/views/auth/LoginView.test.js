import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  back: vi.fn(),
  push: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ back: mocks.back, push: mocks.push })
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn() }
}))

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  register: vi.fn()
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({ setLogin: vi.fn() })
}))

import LoginView from './LoginView.vue'

const stubs = {
  'el-button': { template: '<button><slot /></button>' },
  'el-tabs': { template: '<div><slot /></div>' },
  'el-tab-pane': { template: '<div><slot /></div>' },
  'el-form': { template: '<form><slot /></form>' },
  'el-form-item': { template: '<div><slot /></div>' },
  'el-input': { template: '<input />' }
}

function mountLoginView() {
  return mount(LoginView, { global: { stubs } })
}

describe('LoginView close button', () => {
  beforeEach(() => {
    mocks.back.mockReset()
    mocks.push.mockReset()
  })

  it('returns to the page visited before login', async () => {
    window.history.replaceState({ back: '/flight' }, '', '/login')
    const wrapper = mountLoginView()

    await wrapper.get('[aria-label="返回登录前页面"]').trigger('click')

    expect(mocks.back).toHaveBeenCalledOnce()
    expect(mocks.push).not.toHaveBeenCalled()
  })

  it('returns to home when login is opened without navigation history', async () => {
    window.history.replaceState({ back: null }, '', '/login')
    const wrapper = mountLoginView()

    await wrapper.get('[aria-label="返回登录前页面"]').trigger('click')

    expect(mocks.push).toHaveBeenCalledWith('/')
    expect(mocks.back).not.toHaveBeenCalled()
  })
})
