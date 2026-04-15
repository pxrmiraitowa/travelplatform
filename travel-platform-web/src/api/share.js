import request from '@/utils/request'

export function getShareList(params) {
  return request({
    url: '/public/shares',
    method: 'get',
    params
  })
}

export function getShareDetail(id) {
  return request({
    url: `/public/shares/${id}`,
    method: 'get'
  })
}

export function createShare(data) {
  return request({
    url: '/shares',
    method: 'post',
    data
  })
}

export function getMyShares(params) {
  return request({
    url: '/shares/mine',
    method: 'get',
    params
  })
}

export function uploadShareImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/shares/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
