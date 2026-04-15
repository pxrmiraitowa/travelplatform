<template>
  <div class="admin-page admin-stack">
    <section class="admin-stat-grid">
      <div class="admin-stat-card">
        <span>用户总数</span>
        <strong>{{ dashboard.userCount }}</strong>
      </div>
      <div class="admin-stat-card">
        <span>产品总数</span>
        <strong>{{ dashboard.productCount }}</strong>
      </div>
      <div class="admin-stat-card">
        <span>订单总数</span>
        <strong>{{ dashboard.orderCount }}</strong>
      </div>
      <div class="admin-stat-card">
        <span>近 7 天订单</span>
        <strong>{{ dashboard.recentOrderCount }}</strong>
      </div>
    </section>

    <section class="admin-card">
      <div class="admin-section__head">
        <div>
          <h2>模块说明</h2>
          <p>当前后台已覆盖课程项目核心的用户、产品与订单管理流程。</p>
        </div>
      </div>

      <div class="admin-notes">
        <div>用户管理：支持列表查询、启停用户、管理员角色分配。</div>
        <div>产品管理：支持航班、车次、酒店、房型、旅游产品的统一 CRUD。</div>
        <div>订单管理：支持订单筛选、查看详情、后台改状态与取消订单。</div>
        <div>内容管理：当前仓库未实现分享/评价数据模型，暂未启用该菜单。</div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { getAdminDashboard } from '@/api/admin'

const dashboard = reactive({
  userCount: 0,
  productCount: 0,
  orderCount: 0,
  recentOrderCount: 0
})

onMounted(async () => {
  const response = await getAdminDashboard()
  Object.assign(dashboard, response.data)
})
</script>
