import request from '@/utils/request'

export function getTripPlanList() {
  return request({
    url: '/trip-plans',
    method: 'get'
  })
}

export function createTripPlan(data) {
  return request({
    url: '/trip-plans',
    method: 'post',
    data
  })
}

export function getTripPlanDetail(id) {
  return request({
    url: `/trip-plans/${id}`,
    method: 'get'
  })
}

export function updateTripPlan(id, data) {
  return request({
    url: `/trip-plans/${id}`,
    method: 'put',
    data
  })
}

export function deleteTripPlan(id) {
  return request({
    url: `/trip-plans/${id}`,
    method: 'delete'
  })
}

export function createTripPlanItem(planId, data) {
  return request({
    url: `/trip-plans/${planId}/items`,
    method: 'post',
    data
  })
}

export function updateTripPlanItem(planId, itemId, data) {
  return request({
    url: `/trip-plans/${planId}/items/${itemId}`,
    method: 'put',
    data
  })
}

export function deleteTripPlanItem(planId, itemId) {
  return request({
    url: `/trip-plans/${planId}/items/${itemId}`,
    method: 'delete'
  })
}
