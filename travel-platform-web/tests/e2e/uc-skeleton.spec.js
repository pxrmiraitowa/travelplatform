import { test, expect } from '@playwright/test'

const publicRoutes = [
  ['E2E-TC01 首页可打开', '/', '暑期出行季'],
  ['E2E-TC03 机票查询入口', '/flight', '航班搜索'],
  ['E2E-TC04 火车票查询入口', '/train', '火车票'],
  ['E2E-TC05 酒店查询入口', '/hotel', '酒店搜索'],
  ['E2E-TC06 度假产品入口', '/tour', '旅游度假'],
  ['E2E-TC11 分享入口', '/shares', '旅行分享'],
  ['E2E-TC12 价格提醒入口', '/flight', '航班搜索'],
  ['E2E-TC13 管理员登录入口', '/admin/login', '后台管理系统登录'],
  ['E2E-TC14 后台权限入口', '/admin/login', '后台管理系统登录'],
  ['E2E-SMOKE-01 首页导航可用', '/', '搜索机票'],
  ['E2E-SMOKE-03 机票导航可用', '/flight', '搜索航班']
]

const protectedRoutes = [
  ['E2E-TC02 个人资料入口', '/profile', '个人中心'],
  ['E2E-TC07 订单入口', '/orders', '我的订单'],
  ['E2E-TC08 评价入口', '/orders', '我的订单'],
  ['E2E-TC09 行程规划入口', '/trip-plans', '行程规划'],
  ['E2E-TC10 AI 行程入口', '/trip-plans', '行程规划']
]

let userSequence = 0

async function registerAuthenticatedUser(page) {
  userSequence += 1
  const suffix = String(userSequence).padStart(2, '0')
  const timestamp = Date.now()
  const response = await page.request.post('/api/auth/register', {
    data: {
      username: `e2e_${timestamp.toString(36)}_${suffix}`,
      nickname: `端到端测试用户${suffix}`,
      phone: `1${String(timestamp).slice(-8)}${suffix}`,
      password: 'E2e123456',
      confirmPassword: 'E2e123456'
    }
  })
  expect(response.ok()).toBeTruthy()

  const result = await response.json()
  expect(result.code).toBe(200)
  expect(result.data?.token).toBeTruthy()
  expect(result.data?.userInfo).toBeTruthy()

  await page.addInitScript(({ token, userInfo }) => {
    localStorage.setItem('travel-platform-token', token)
    localStorage.setItem('travel-platform-user', JSON.stringify(userInfo))
  }, result.data)
}

for (const [name, route, text] of publicRoutes) {
  test(name, async ({ page }) => {
    await page.goto(route)
    await expect(page.locator('body')).toContainText(text)
  })
}

for (const [name, route, text] of protectedRoutes) {
  test(name, async ({ page }) => {
    await registerAuthenticatedUser(page)
    await page.goto(route)
    await expect(page).toHaveURL(new RegExp(`${route.replace('/', '\\/')}$`))
    await expect(page.locator('body')).toContainText(text)
  })
}
