import request from '@/utils/request'

export function getAdminDashboard() {
  return request({
    url: '/admin/dashboard',
    method: 'get'
  })
}

export function getAdminUsers(params) {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

export function getAdminUserDetail(id) {
  return request({
    url: `/admin/users/${id}`,
    method: 'get'
  })
}

export function updateAdminUserStatus(id, data) {
  return request({
    url: `/admin/users/${id}/status`,
    method: 'put',
    data
  })
}

export function updateAdminUserRoles(id, data) {
  return request({
    url: `/admin/users/${id}/roles`,
    method: 'put',
    data
  })
}

export function getAdminRoles() {
  return request({
    url: '/admin/roles',
    method: 'get'
  })
}

export function getAdminFlights(params) {
  return request({ url: '/admin/flights', method: 'get', params })
}

export function createAdminFlight(data) {
  return request({ url: '/admin/flights', method: 'post', data })
}

export function updateAdminFlight(id, data) {
  return request({ url: `/admin/flights/${id}`, method: 'put', data })
}

export function deleteAdminFlight(id) {
  return request({ url: `/admin/flights/${id}`, method: 'delete' })
}

export function getAdminTrains(params) {
  return request({ url: '/admin/trains', method: 'get', params })
}

export function createAdminTrain(data) {
  return request({ url: '/admin/trains', method: 'post', data })
}

export function updateAdminTrain(id, data) {
  return request({ url: `/admin/trains/${id}`, method: 'put', data })
}

export function deleteAdminTrain(id) {
  return request({ url: `/admin/trains/${id}`, method: 'delete' })
}

export function getAdminHotels(params) {
  return request({ url: '/admin/hotels', method: 'get', params })
}

export function createAdminHotel(data) {
  return request({ url: '/admin/hotels', method: 'post', data })
}

export function updateAdminHotel(id, data) {
  return request({ url: `/admin/hotels/${id}`, method: 'put', data })
}

export function deleteAdminHotel(id) {
  return request({ url: `/admin/hotels/${id}`, method: 'delete' })
}

export function getAdminHotelRooms(params) {
  return request({ url: '/admin/hotel-rooms', method: 'get', params })
}

export function createAdminHotelRoom(data) {
  return request({ url: '/admin/hotel-rooms', method: 'post', data })
}

export function updateAdminHotelRoom(id, data) {
  return request({ url: `/admin/hotel-rooms/${id}`, method: 'put', data })
}

export function deleteAdminHotelRoom(id) {
  return request({ url: `/admin/hotel-rooms/${id}`, method: 'delete' })
}

export function getAdminTours(params) {
  return request({ url: '/admin/tours', method: 'get', params })
}

export function createAdminTour(data) {
  return request({ url: '/admin/tours', method: 'post', data })
}

export function updateAdminTour(id, data) {
  return request({ url: `/admin/tours/${id}`, method: 'put', data })
}

export function deleteAdminTour(id) {
  return request({ url: `/admin/tours/${id}`, method: 'delete' })
}

export function getAdminOrders(params) {
  return request({ url: '/admin/orders', method: 'get', params })
}

export function getAdminOrderDetail(id) {
  return request({ url: `/admin/orders/${id}`, method: 'get' })
}

export function updateAdminOrderStatus(id, data) {
  return request({ url: `/admin/orders/${id}/status`, method: 'put', data })
}

export function cancelAdminOrder(id) {
  return request({ url: `/admin/orders/${id}/cancel`, method: 'post' })
}

export function getAdminShares(params) {
  return request({ url: '/admin/shares', method: 'get', params })
}

export function deleteAdminShare(id) {
  return request({ url: `/admin/shares/${id}`, method: 'delete' })
}

export function getAdminReviews(params) {
  return request({ url: '/admin/reviews', method: 'get', params })
}

export function deleteAdminReview(id) {
  return request({ url: `/admin/reviews/${id}`, method: 'delete' })
}
