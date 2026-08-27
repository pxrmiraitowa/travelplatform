import request from '@/utils/request'

export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function adminLogin(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function getCurrentAdmin() {
  return request({
    url: '/users/me',
    method: 'get'
  })
}

export function getHealth() {
  return request({
    url: '/public/health',
    method: 'get'
  })
}
