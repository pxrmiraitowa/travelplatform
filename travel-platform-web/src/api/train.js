import request from '@/utils/request'

export function searchTrains(params) {
  return request({
    url: '/public/trains',
    method: 'get',
    params
  })
}

export function getTrainDetail(id) {
  return request({
    url: `/public/trains/${id}`,
    method: 'get'
  })
}
