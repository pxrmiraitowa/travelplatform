import request from '@/utils/request'

export function searchHotels(params) {
  return request({
    url: '/public/hotels',
    method: 'get',
    params
  })
}

export function getHotelDetail(id) {
  return request({
    url: `/public/hotels/${id}`,
    method: 'get'
  })
}
