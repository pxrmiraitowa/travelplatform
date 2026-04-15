import request from '@/utils/request'

export function getPriceAlerts() {
  return request({
    url: '/price-alerts',
    method: 'get'
  })
}

export function createPriceAlert(data) {
  return request({
    url: '/price-alerts',
    method: 'post',
    data
  })
}

export function deletePriceAlert(id) {
  return request({
    url: `/price-alerts/${id}`,
    method: 'delete'
  })
}
