import request from '@/utils/request'

export function createReview(data) {
  return request({
    url: '/reviews',
    method: 'post',
    data
  })
}

export function getReviewableOrders(params) {
  return request({
    url: '/orders/reviewable',
    method: 'get',
    params
  })
}

export function getOrderReview(id) {
  return request({
    url: `/orders/${id}/review`,
    method: 'get'
  })
}
