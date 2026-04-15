import request from '@/utils/request'

export function getUserContacts() {
  return request({
    url: '/user-contacts',
    method: 'get'
  })
}

export function createUserContact(data) {
  return request({
    url: '/user-contacts',
    method: 'post',
    data
  })
}

export function updateUserContact(id, data) {
  return request({
    url: `/user-contacts/${id}`,
    method: 'put',
    data
  })
}

export function deleteUserContact(id) {
  return request({
    url: `/user-contacts/${id}`,
    method: 'delete'
  })
}
