import { test, expect } from '@playwright/test'
test('E2E-FAILURE-SAMPLE 故意失败样例', async () => {
  expect(true).toBe(false)
})
