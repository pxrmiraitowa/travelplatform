import request from '@/utils/request'

export function searchFlights(params) {
  return request({
    url: '/public/flights',
    method: 'get',
    params
  })
}

export function getFlightDetail(id) {
  return request({
    url: `/public/flights/${id}`,
    method: 'get'
  })
}
