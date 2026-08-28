import { beforeAll, describe, expect, it } from 'vitest'
import {
  api,
  adminLogin,
  createOrder,
  firstRecord,
  login,
  records,
  uniqueSuffix,
  uploadTinyPng,
  waitForGateway
} from './api-client.js'

describe.sequential('end-to-end business scenario regression through gateway', () => {
  const suffix = uniqueSuffix().slice(-8)
  const username = `e${suffix}`
  const password = '123456'
  let userToken
  let adminToken
  let currentUser
  let createdReviewId
  let createdShareId

  beforeAll(async () => {
    await waitForGateway()
    adminToken = (await adminLogin()).token
  })

  it('UC01 registers, logs in, and reads current user', async () => {
    const registered = await api('/auth/register', {
      method: 'POST',
      body: {
        username,
        nickname: `端到端用户${suffix}`,
        phone: `138${suffix}`,
        password,
        confirmPassword: password
      }
    })
    expect(registered.data.token).toBeTruthy()

    const loggedIn = await login(username, password)
    userToken = loggedIn.token

    const me = await api('/users/me', { token: userToken })
    currentUser = me.data
    expect(currentUser.username).toBe(username)

    expect(userToken).toBeTruthy()
  })

  it('UC02 updates profile and manages contacts', async () => {
    const updated = await api('/users/me', {
      method: 'PUT',
      token: userToken,
      body: {
        nickname: `资料回归${suffix}`,
        realName: '测试用户',
        phone: `138${suffix}`,
        email: `${username}@example.com`,
        gender: 1,
        avatar: ''
      }
    })
    expect(updated.data.nickname).toContain('资料回归')

    const created = await api('/user-contacts', {
      method: 'POST',
      token: userToken,
      body: {
        name: '测试联系人',
        phone: '13800000000',
        idCard: '110101199001010011',
        contactType: 1,
        isDefault: 1,
        remark: 'created by regression'
      }
    })
    const contactId = created.data.id
    expect(contactId).toBeTruthy()

    expect(records(await api('/user-contacts', { token: userToken })).some(item => item.id === contactId)).toBe(true)
    const changed = await api(`/user-contacts/${contactId}`, {
      method: 'PUT',
      token: userToken,
      body: {
        name: '测试联系人已修改',
        phone: '13800000001',
        idCard: '110101199001010011',
        contactType: 1,
        isDefault: 0,
        remark: 'updated by regression'
      }
    })
    expect(changed.data.name).toContain('已修改')
    await api(`/user-contacts/${contactId}`, { method: 'DELETE', token: userToken })
  })

  it('UC03 covers flight query, detail, coupon order, and cancel', async () => {
    const flight = firstRecord(await api('/public/flights?pageNum=1&pageSize=10'), 'flights')
    expect((await api(`/public/flights/${flight.id}`)).data.id).toBe(flight.id)
    const order = await createOrder(userToken, {
      productType: 'FLIGHT',
      productId: flight.id,
      couponId: 2,
      travelDate: '2030-07-02'
    })
    expect(Number(order.discountAmount)).toBeGreaterThan(0)
    expect((await api(`/orders/${order.id}`, { token: userToken })).data.id).toBe(order.id)
    expect((await api(`/orders/${order.id}/cancel`, { method: 'POST', token: userToken })).data.orderStatus).toBe(40)
  })

  it('UC04 covers train query, detail, order, pay, and refund', async () => {
    const train = firstRecord(await api('/public/trains?pageNum=1&pageSize=10'), 'trains')
    const detail = (await api(`/public/trains/${train.id}`)).data
    const seat = detail.seatOptions.find(item => item.available) || detail.seatOptions[0]
    const order = await createOrder(userToken, {
      productType: 'TRAIN',
      productId: train.id,
      variantName: seat.seatType,
      travelDate: '2030-07-03'
    })
    expect((await api(`/orders/${order.id}/pay`, { method: 'POST', token: userToken })).data.orderStatus).toBe(20)
    expect((await api(`/orders/${order.id}/refund`, {
      method: 'POST',
      token: userToken,
      body: { reason: '端到端回归退款' }
    })).data.orderStatus).toBe(50)
  })

  it('UC05 covers hotel query, room detail, reservation, and cancel', async () => {
    const hotel = firstRecord(await api('/public/hotels?pageNum=1&pageSize=10'), 'hotels')
    const detail = (await api(`/public/hotels/${hotel.id}`)).data
    const room = detail.roomList[0]
    const order = await createOrder(userToken, {
      productType: 'HOTEL',
      productId: hotel.id,
      variantId: room.id,
      variantName: room.roomName,
      travelDate: '2030-07-04'
    })
    expect(order.productName).toBeTruthy()
    await api(`/orders/${order.id}/cancel`, { method: 'POST', token: userToken })
  })

  it('UC06 covers tour browsing, detail, order, and cancel', async () => {
    const tour = firstRecord(await api('/public/tours?pageNum=1&pageSize=10'), 'tours')
    const detail = (await api(`/public/tours/${tour.id}`)).data
    const order = await createOrder(userToken, {
      productType: 'TOUR',
      productId: tour.id,
      travelDate: detail.travelDateOptions[0] || '2030-07-10'
    })
    expect(order.bizType).toBe('TOUR')
    await api(`/orders/${order.id}/cancel`, { method: 'POST', token: userToken })
  })

  it('UC07 queries order list/detail and exercises user order state changes', async () => {
    const order = await createOrder(userToken)
    const list = await api('/orders?pageNum=1&pageSize=20', { token: userToken })
    expect(records(list).some(item => item.id === order.id)).toBe(true)
    expect((await api(`/orders/${order.id}`, { token: userToken })).data.id).toBe(order.id)
    await api(`/orders/${order.id}/cancel`, { method: 'POST', token: userToken })
  })

  it('UC08 completes an order and submits a review', async () => {
    const order = await createOrder(userToken)
    await api(`/orders/${order.id}/pay`, { method: 'POST', token: userToken })
    await api(`/orders/${order.id}/complete`, { method: 'POST', token: userToken })

    const reviewable = await api('/orders/reviewable?pageNum=1&pageSize=20', { token: userToken })
    expect(records(reviewable).some(item => item.orderId === order.id || item.id === order.id)).toBe(true)

    const review = await api('/reviews', {
      method: 'POST',
      token: userToken,
      body: { orderId: order.id, rating: 5, content: '端到端回归评价' }
    })
    createdReviewId = review.data.id
    expect(createdReviewId).toBeTruthy()
    expect((await api(`/orders/${order.id}/review`, { token: userToken })).data.content).toContain('端到端回归')
  })

  it('UC09 manages a manual trip plan and daily items', async () => {
    const plan = await api('/trip-plans', {
      method: 'POST',
      token: userToken,
      body: {
        planName: `手动行程${suffix}`,
        totalDays: 2,
        startDate: '2030-08-01',
        remark: 'manual e2e'
      }
    })
    const planId = plan.data.id
    expect(planId).toBeTruthy()
    expect(records(await api('/trip-plans', { token: userToken })).some(item => item.id === planId)).toBe(true)

    const updatedPlan = await api(`/trip-plans/${planId}`, {
      method: 'PUT',
      token: userToken,
      body: {
        planName: `手动行程已修改${suffix}`,
        totalDays: 2,
        startDate: '2030-08-01',
        remark: 'updated manual e2e'
      }
    })
    expect(updatedPlan.data.planName).toContain('已修改')

    const item = await api(`/trip-plans/${planId}/items`, {
      method: 'POST',
      token: userToken,
      body: {
        dayNo: 1,
        destination: '上海外滩',
        hotel: '上海外滩精选酒店',
        transportType: '地铁',
        remark: '上午游览'
      }
    })
    const itemId = item.data.id
    expect(itemId).toBeTruthy()
    await api(`/trip-plans/${planId}/items/${itemId}`, {
      method: 'PUT',
      token: userToken,
      body: {
        dayNo: 1,
        destination: '上海外滩',
        hotel: '上海外滩精选酒店',
        transportType: '步行',
        remark: '调整交通方式'
      }
    })
    await api(`/trip-plans/${planId}/items/${itemId}`, { method: 'DELETE', token: userToken })
    await api(`/trip-plans/${planId}`, { method: 'DELETE', token: userToken })
  })

  it('UC10 previews and saves a generated trip plan', async () => {
    const preview = await api('/trip-plans/ai-preview', {
      method: 'POST',
      token: userToken,
      body: {
        destination: '上海',
        totalDays: 1,
        startDate: '2030-08-03',
        preferences: ['城市观光']
      }
    })
    expect(preview.data.days.length).toBeGreaterThan(0)

    const firstDay = preview.data.days[0]
    const saved = await api('/trip-plans/ai-save', {
      method: 'POST',
      token: userToken,
      body: {
        planName: `生成行程${suffix}`,
        destination: '上海',
        totalDays: 1,
        startDate: '2030-08-03',
        preferences: ['城市观光'],
        days: [{
          dayNo: firstDay.dayNo || 1,
          attractionIds: (firstDay.attractions || []).slice(0, 1).map(item => item.id),
          reason: '端到端回归保存'
        }]
      }
    })
    expect(saved.data.id).toBeTruthy()
    await api(`/trip-plans/${saved.data.id}`, { method: 'DELETE', token: userToken })
  })

  it('UC11 uploads, publishes, lists, and opens a travel share', async () => {
    const imageUrl = await uploadTinyPng('/shares/upload', userToken)
    const share = await api('/shares', {
      method: 'POST',
      token: userToken,
      body: {
        title: `游记回归${suffix}`,
        summary: '游记端到端摘要',
        content: '游记端到端正文',
        imageUrls: [imageUrl]
      }
    })
    createdShareId = share.data.id
    expect(createdShareId).toBeTruthy()
    expect(records(await api('/shares/mine?pageNum=1&pageSize=20', { token: userToken })).some(item => item.id === createdShareId)).toBe(true)
    expect((await api(`/public/shares/${createdShareId}`)).data.title).toContain('游记回归')
  })

  it('UC12 compares prices and manages price alerts', async () => {
    expect((await api('/public/price-compare/flights/1')).data.lowPriceLabel).toBeTruthy()
    const alert = await api('/price-alerts', {
      method: 'POST',
      token: userToken,
      body: {
        productType: 'FLIGHT',
        productId: 1,
        targetPrice: 500,
        remark: '端到端价格提醒'
      }
    })
    const alertId = alert.data.id
    expect(alertId).toBeTruthy()
    expect(records(await api('/price-alerts', { token: userToken })).some(item => item.id === alertId)).toBe(true)
    await api(`/price-alerts/${alertId}`, { method: 'DELETE', token: userToken })
  })

  it('UC13 lets admin manage products and upload product images', async () => {
    const imageUrl = await uploadTinyPng('/admin/media/upload', adminToken)
    expect(imageUrl).toContain('/product/')

    const flight = await api('/admin/flights', {
      method: 'POST',
      token: adminToken,
      body: {
        flightNo: `RG${suffix}`,
        airlineName: '回归航空',
        departureCity: '上海',
        arrivalCity: '北京',
        departureAirport: '虹桥T2',
        arrivalAirport: '首都T2',
        departureTime: '2030-09-01T08:00:00',
        arrivalTime: '2030-09-01T10:00:00',
        price: 800,
        stock: 5,
        cabinClass: 'Economy',
        baggagePolicy: '20KG',
        refundPolicy: '可退改',
        status: 1
      }
    })
    await api(`/admin/flights/${flight.data.id}`, {
      method: 'PUT',
      token: adminToken,
      body: { ...flight.data, airlineName: '回归航空已修改', departureTime: '2030-09-01T08:00:00', arrivalTime: '2030-09-01T10:00:00', status: 1 }
    })
    await api(`/admin/flights/${flight.data.id}`, { method: 'DELETE', token: adminToken })

    const train = await api('/admin/trains', {
      method: 'POST',
      token: adminToken,
      body: {
        trainNo: `G${suffix}`,
        trainType: '高铁',
        departureCity: '上海',
        arrivalCity: '杭州',
        departureStation: '上海虹桥',
        arrivalStation: '杭州东',
        departureTime: '2030-09-02T08:00:00',
        arrivalTime: '2030-09-02T09:00:00',
        durationMinutes: 60,
        businessPrice: 300,
        firstClassPrice: 200,
        secondClassPrice: 100,
        businessStock: 3,
        firstClassStock: 5,
        secondClassStock: 10,
        status: 1
      }
    })
    await api(`/admin/trains/${train.data.id}`, { method: 'DELETE', token: adminToken })

    const hotel = await api('/admin/hotels', {
      method: 'POST',
      token: adminToken,
      body: {
        hotelName: `回归酒店${suffix}`,
        city: '上海',
        district: '徐汇区',
        address: '测试路1号',
        description: '端到端回归酒店',
        starLevel: 4,
        coverImage: imageUrl,
        detailImages: '[]',
        checkInTime: '14:00',
        checkOutTime: '12:00',
        status: 1
      }
    })
    const room = await api('/admin/hotel-rooms', {
      method: 'POST',
      token: adminToken,
      body: {
        hotelId: hotel.data.id,
        roomName: '回归大床房',
        bedType: '大床',
        breakfast: '双早',
        roomArea: '30平方米',
        guestCount: 2,
        price: 499,
        stock: 5,
        cancelRule: '可取消',
        status: 1
      }
    })
    await api(`/admin/hotel-rooms/${room.data.id}`, { method: 'DELETE', token: adminToken })
    await api(`/admin/hotels/${hotel.data.id}`, { method: 'DELETE', token: adminToken })

    const tour = await api('/admin/tours', {
      method: 'POST',
      token: adminToken,
      body: {
        packageName: `回归旅游${suffix}`,
        destination: '三亚',
        departureCity: '深圳',
        days: 3,
        price: 1999,
        stock: 8,
        travelDates: '2030-09-10,2030-09-17',
        description: '端到端回归旅游产品',
        coverImage: imageUrl,
        detailImages: '[]',
        status: 1
      }
    })
    await api(`/admin/tours/${tour.data.id}`, { method: 'DELETE', token: adminToken })
  })

  it('UC14 lets admin manage users, orders, shares, and reviews', async () => {
    const dashboard = await api('/admin/dashboard', { token: adminToken })
    expect(Number(dashboard.data.userCount)).toBeGreaterThan(0)

    const userList = await api(`/admin/users?keyword=${username}&pageNum=1&pageSize=10`, { token: adminToken })
    const managedUser = records(userList).find(item => item.username === username)
    expect(managedUser.id).toBe(currentUser.id)
    await api(`/admin/users/${managedUser.id}/status`, { method: 'PUT', token: adminToken, body: { status: 1 } })
    await api(`/admin/users/${managedUser.id}/roles`, { method: 'PUT', token: adminToken, body: { roleCodes: ['ROLE_USER'] } })
    expect(records(await api('/admin/roles', { token: adminToken })).length).toBeGreaterThan(0)

    const order = await createOrder(userToken)
    const adminOrders = await api('/admin/orders?pageNum=1&pageSize=20', { token: adminToken })
    expect(records(adminOrders).some(item => item.id === order.id)).toBe(true)
    expect((await api(`/admin/orders/${order.id}`, { token: adminToken })).data.id).toBe(order.id)
    await api(`/admin/orders/${order.id}/status`, { method: 'PUT', token: adminToken, body: { orderStatus: 10 } })
    await api(`/admin/orders/${order.id}/cancel`, { method: 'POST', token: adminToken })

    const shares = await api('/admin/shares?pageNum=1&pageSize=20', { token: adminToken })
    expect(records(shares).length).toBeGreaterThanOrEqual(0)
    if (createdShareId) {
      await api(`/admin/shares/${createdShareId}`, { method: 'DELETE', token: adminToken })
      createdShareId = null
    }

    const reviews = await api('/admin/reviews?pageNum=1&pageSize=20', { token: adminToken })
    expect(records(reviews).length).toBeGreaterThanOrEqual(0)
    if (createdReviewId) {
      await api(`/admin/reviews/${createdReviewId}`, { method: 'DELETE', token: adminToken })
      createdReviewId = null
    }
  })

  afterAll(async () => {
    if (createdShareId) {
      await api(`/admin/shares/${createdShareId}`, { method: 'DELETE', token: adminToken }).catch(() => {})
    }
    if (createdReviewId) {
      await api(`/admin/reviews/${createdReviewId}`, { method: 'DELETE', token: adminToken }).catch(() => {})
    }
    if (userToken) {
      await api('/auth/logout', { method: 'POST', token: userToken }).catch(() => {})
    }
  })
})
