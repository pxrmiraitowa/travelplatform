import request from '@/utils/request'

export function createFlightOrder(data) {
  return request({
    url: '/orders',
    method: 'post',
    data
  })
}

export function createTrainOrder(data) {
  return request({
    url: '/orders',
    method: 'post',
    data
  })
}

export function createHotelOrder(data) {
  return request({
    url: '/orders',
    method: 'post',
    data
  })
}

export function createTourOrder(data) {
  return request({
    url: '/orders',
    method: 'post',
    data
  })
}

export function getOrderList(params) {
  return request({
    url: '/orders',
    method: 'get',
    params
  })
}

export function getOrderDetail(id) {
  return request({
    url: `/orders/${id}`,
    method: 'get'
  })
}

export function getOrderReview(id) {
  return request({
    url: `/orders/${id}/review`,
    method: 'get'
  })
}

export function cancelOrder(id) {
  return request({
    url: `/orders/${id}/cancel`,
    method: 'post'
  })
}

export function refundOrder(id, reason) {
  return request({
    url: `/orders/${id}/refund`,
    method: 'post',
    data: { reason }
  })
}
