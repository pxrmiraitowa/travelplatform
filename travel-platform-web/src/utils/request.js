import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('travel-platform-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result.code !== 200) {
      ElMessage.error(result.message || '请求失败')
      return Promise.reject(result)
    }
    return result
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      try {
        useUserStore().clearLogin()
      } catch (storeError) {
        localStorage.removeItem('travel-platform-token')
        localStorage.removeItem('travel-platform-user')
      }
      ElMessage.error(error.response?.data?.message || '登录已失效，请重新登录')
      const currentPath = router.currentRoute.value?.fullPath || '/'
      const loginPath = currentPath.startsWith('/admin') ? '/admin/login' : '/login'
      router.push({
        path: loginPath,
        query: currentPath === loginPath ? undefined : { redirect: currentPath }
      })
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default service
