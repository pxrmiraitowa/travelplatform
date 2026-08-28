import { beforeAll, describe, expect, it } from 'vitest'
import {
  api,
  DIRECT_SERVICE_BASES,
  adminLogin,
  firstRecord,
  login,
  records,
  uniqueSuffix,
  waitForGateway
} from './api-client.js'

describe.sequential('public API regression through gateway', () => {
  let userToken
  let adminToken
  let createdShareId

  beforeAll(async () => {
    await waitForGateway()
    userToken = (await login()).token
    adminToken = (await adminLogin()).token
  })

  it('covers health and version endpoints for every deployed backend service', async () => {
    for (const [name, base] of Object.entries(DIRECT_SERVICE_BASES)) {
      const health = await api('/public/health', { base })
      expect(health.data.status, `${name} health`).toBe('UP')
      if (name !== 'gateway') {
        const version = await api('/public/version', { base })
        expect(version.data.service || version.data.serviceName, `${name} version service name`).toBeTruthy()
        expect(version.data.version, `${name} version`).toBeTruthy()
      }
    }
  })

  it('covers public product list, detail, and query APIs', async () => {
    const flights = await api('/public/flights?departureCity=上海&arrivalCity=北京&pageNum=1&pageSize=10')
    const flight = firstRecord(flights, 'flights')
    expect((await api(`/public/flights/${flight.id}`)).data.flightNo).toBeTruthy()

    const trains = await api('/public/trains?departureCity=上海&arrivalCity=杭州&pageNum=1&pageSize=10')
    const train = firstRecord(trains, 'trains')
    expect((await api(`/public/trains/${train.id}`)).data.seatOptions.length).toBeGreaterThan(0)

    const hotels = await api('/public/hotels?city=上海&pageNum=1&pageSize=10')
    const hotel = firstRecord(hotels, 'hotels')
    expect((await api(`/public/hotels/${hotel.id}`)).data.roomList.length).toBeGreaterThan(0)

    const tours = await api('/public/tours?destination=三亚&pageNum=1&pageSize=10')
    const tour = firstRecord(tours, 'tours')
    expect((await api(`/public/tours/${tour.id}`)).data.travelDateOptions.length).toBeGreaterThan(0)
  })

  it('covers public price comparison APIs and coupon data', async () => {
    const flightCompare = await api('/public/price-compare/flights/1')
    expect(flightCompare.data.productType).toBe('FLIGHT')
    expect(records({ data: { records: flightCompare.data.compareItems } }).length).toBeGreaterThan(0)
    expect(flightCompare.data.couponList.length).toBeGreaterThan(0)

    expect((await api('/public/price-compare/hotels/1')).data.productType).toBe('HOTEL')
    expect((await api('/public/price-compare/tours/1')).data.productType).toBe('TOUR')
  })

  it('covers public share list and detail APIs', async () => {
    const suffix = uniqueSuffix()
    const created = await api('/shares', {
      method: 'POST',
      token: userToken,
      body: {
        title: `公开分享回归 ${suffix}`,
        summary: '公开分享接口回归摘要',
        content: '公开分享接口回归正文',
        imageUrls: ['/api/public/uploads/share/regression.png']
      }
    })
    createdShareId = created.data.id
    expect(createdShareId).toBeTruthy()

    const list = await api('/public/shares?pageNum=1&pageSize=10')
    expect(records(list).some(item => item.id === createdShareId)).toBe(true)

    const detail = await api(`/public/shares/${createdShareId}`)
    expect(detail.data.title).toContain('公开分享回归')

    await api(`/admin/shares/${createdShareId}`, { method: 'DELETE', token: adminToken })
    createdShareId = null
  })

  it('covers public auth APIs', async () => {
    const suffix = uniqueSuffix().slice(-8)
    const username = `u${suffix}`
    const registered = await api('/auth/register', {
      method: 'POST',
      body: {
        username,
        nickname: `注册回归${suffix}`,
        phone: `139${suffix}`,
        password: '123456',
        confirmPassword: '123456'
      }
    })
    expect(registered.data.token).toBeTruthy()

    const loggedIn = await api('/auth/login', {
      method: 'POST',
      body: { username, password: '123456' }
    })
    expect(loggedIn.data.userInfo.username).toBe(username)
  })

  afterAll(async () => {
    if (createdShareId) {
      await api(`/admin/shares/${createdShareId}`, { method: 'DELETE', token: adminToken }).catch(() => {})
    }
  })
})
