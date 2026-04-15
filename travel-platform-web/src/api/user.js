import request from '@/utils/request'

export function getCurrentUser() {
  return request({
    url: '/users/me',
    method: 'get'
  })
}

export function updateCurrentUser(data) {
  return request({
    url: '/users/me',
    method: 'put',
    data
  })
}
