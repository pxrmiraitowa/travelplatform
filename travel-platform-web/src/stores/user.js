import { defineStore } from 'pinia'
import { getCurrentUser, updateCurrentUser } from '@/api/user'
import { logout as logoutApi } from '@/api/auth'

const TOKEN_KEY = 'travel-platform-token'
const USER_KEY = 'travel-platform-user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => Array.isArray(state.userInfo?.roleCodes) && state.userInfo.roleCodes.includes('ROLE_ADMIN')
  },
  actions: {
    setLogin(token, userInfo) {
      this.token = token
      this.userInfo = userInfo
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
    },
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
    },
    async fetchCurrentUser() {
      if (!this.token) {
        return null
      }
      const response = await getCurrentUser()
      this.setUserInfo(response.data)
      return response.data
    },
    async updateProfile(data) {
      const response = await updateCurrentUser(data)
      this.setUserInfo(response.data)
      return response.data
    },
    clearLogin() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
    async logoutAction() {
      if (this.token) {
        try {
          await logoutApi()
        } catch (error) {
          // Ignore logout API failures and still clear local auth state.
        }
      }
      this.clearLogin()
    }
  }
})
