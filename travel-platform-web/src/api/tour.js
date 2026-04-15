import request from '@/utils/request'

export function getTourList(params) {
  return request({
    url: '/public/tours',
    method: 'get',
    params
  })
}

export function getTourDetail(id) {
  return request({
    url: `/public/tours/${id}`,
    method: 'get'
  })
}
