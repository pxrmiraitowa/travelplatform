import request from '@/utils/request'

export function getHotelPriceCompare(id) {
  return request({
    url: `/public/price-compare/hotels/${id}`,
    method: 'get'
  })
}

export function getFlightPriceCompare(id) {
  return request({
    url: `/public/price-compare/flights/${id}`,
    method: 'get'
  })
}

export function getTourPriceCompare(id) {
  return request({
    url: `/public/price-compare/tours/${id}`,
    method: 'get'
  })
}
