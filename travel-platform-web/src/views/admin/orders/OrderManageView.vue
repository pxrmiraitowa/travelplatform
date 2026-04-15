<template>
  <div class="admin-page">
    <section class="admin-card">
      <div class="admin-section__head">
        <div>
          <h2>订单管理</h2>
          <p>支持按业务类型和状态筛选，查看详情并执行后台状态维护。</p>
        </div>
      </div>

      <div class="admin-toolbar">
        <el-input v-model="query.keyword" placeholder="订单号 / 用户 / 产品摘要" clearable class="admin-filter" />
        <el-select v-model="query.bizType" placeholder="业务类型" clearable class="admin-filter">
          <el-option label="航班" value="FLIGHT" />
          <el-option label="车次" value="TRAIN" />
          <el-option label="酒店" value="HOTEL" />
          <el-option label="旅游" value="TOUR" />
        </el-select>
        <el-select v-model="query.status" placeholder="订单状态" clearable class="admin-filter">
          <el-option label="待支付" :value="10" />
          <el-option label="待出行" :value="20" />
          <el-option label="已完成" :value="30" />
          <el-option label="已取消" :value="40" />
        </el-select>
        <el-button type="primary" @click="loadOrders">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table :data="orders" border v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column label="用户" min-width="140">
          <template #default="{ row }">
            {{ row.nickname || row.username }}
          </template>
        </el-table-column>
        <el-table-column prop="bizType" label="类型" width="110" />
        <el-table-column prop="summaryTitle" label="摘要" min-width="220" />
        <el-table-column prop="totalAmount" label="金额" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.orderStatus)">{{ statusLabel(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/admin/orders/${row.id}`)">详情</el-button>
            <el-button v-if="row.orderStatus === 10" link type="success" @click="changeStatus(row.id, 20)">标记已支付</el-button>
            <el-button v-if="row.orderStatus === 20" link type="warning" @click="changeStatus(row.id, 30)">标记完成</el-button>
            <el-button
              v-if="row.orderStatus === 10 || row.orderStatus === 20"
              link
              type="danger"
              @click="cancelOrder(row.id)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { cancelAdminOrder, getAdminOrders, updateAdminOrderStatus } from '@/api/admin'

const loading = ref(false)
const orders = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  bizType: '',
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

function statusLabel(status) {
  return {
    10: '待支付',
    20: '待出行',
    30: '已完成',
    40: '已取消'
  }[status] || status
}

function statusTagType(status) {
  return {
    10: 'warning',
    20: 'primary',
    30: 'success',
    40: 'info'
  }[status] || 'info'
}

async function loadOrders() {
  loading.value = true
  try {
    const response = await getAdminOrders(query)
    orders.value = response.data.records || []
    total.value = response.data.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { keyword: '', bizType: '', status: undefined, pageNum: 1, pageSize: 10 })
  loadOrders()
}

async function changeStatus(id, orderStatus) {
  await updateAdminOrderStatus(id, { orderStatus })
  ElMessage.success('订单状态已更新')
  loadOrders()
}

async function cancelOrder(id) {
  await cancelAdminOrder(id)
  ElMessage.success('订单已取消')
  loadOrders()
}

function handlePageChange(page) {
  query.pageNum = page
  loadOrders()
}

onMounted(loadOrders)
</script>
