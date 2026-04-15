<template>
  <div class="order-page">
    <SectionCard title="统一订单中心" description="在一个列表中查看机票、火车票、酒店和旅游产品订单，并支持完成后评价。">
      <div class="toolbar">
        <el-select v-model="filters.bizType" clearable placeholder="全部类型" class="filter-select" @change="handleSearch">
          <el-option label="机票订单" value="FLIGHT" />
          <el-option label="火车票订单" value="TRAIN" />
          <el-option label="酒店订单" value="HOTEL" />
          <el-option label="旅游订单" value="TOUR" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="全部状态" class="filter-select" @change="handleSearch">
          <el-option label="待支付" :value="10" />
          <el-option label="待出行" :value="20" />
          <el-option label="已完成" :value="30" />
          <el-option label="已取消" :value="40" />
        </el-select>
        <el-button type="primary" @click="handleSearch">刷新订单</el-button>
      </div>

      <div class="order-table-wrap">
        <el-table :data="orders" border stripe v-loading="loading">
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column label="订单类型" min-width="110">
            <template #default="{ row }">{{ bizTypeText(row.bizType) }}</template>
          </el-table-column>
          <el-table-column prop="summaryTitle" label="产品信息" min-width="180" />
          <el-table-column prop="summarySubtitle" label="摘要" min-width="220" />
          <el-table-column label="出行日期" min-width="120">
            <template #default="{ row }">{{ row.travelDate || '--' }}</template>
          </el-table-column>
          <el-table-column label="金额" min-width="110">
            <template #default="{ row }">
              <span class="price-text">¥{{ Number(row.totalAmount || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="110">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.orderStatus)">{{ statusText(row.orderStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评价状态" min-width="110">
            <template #default="{ row }">
              <el-tag :type="row.reviewed ? 'success' : 'info'">{{ row.reviewed ? '已评价' : '未评价' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goToOrderDetail(row.id)">查看详情</el-button>
              <el-button v-if="[10, 20].includes(row.orderStatus)" link type="danger" @click="handleCancel(row)">取消订单</el-button>
              <el-button v-if="row.orderStatus === 30 && !row.reviewed" link type="warning" @click="openReviewDialog(row)">去评价</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </div>
    </SectionCard>

    <ReviewDialog v-model="reviewDialogVisible" :order="selectedOrder" @success="loadOrders" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import SectionCard from '@/components/SectionCard.vue'
import ReviewDialog from '@/components/review/ReviewDialog.vue'
import { cancelOrder, getOrderList } from '@/api/order'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const reviewDialogVisible = ref(false)
const selectedOrder = ref(null)

const filters = reactive({
  bizType: '',
  status: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

async function loadOrders() {
  loading.value = true
  try {
    const response = await getOrderList({
      bizType: filters.bizType || undefined,
      status: filters.status ?? undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    orders.value = response.data.records || []
    pagination.total = response.data.total || 0
    pagination.pageNum = response.data.pageNum || 1
    pagination.pageSize = response.data.pageSize || 10
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadOrders()
}

function handlePageChange(page) {
  pagination.pageNum = page
  loadOrders()
}

function goToOrderDetail(id) {
  router.push(`/orders/${id}`)
}

function openReviewDialog(row) {
  selectedOrder.value = row
  reviewDialogVisible.value = true
}

async function handleCancel(row) {
  await ElMessageBox.confirm(`确认取消订单“${row.orderNo}”吗？`, '取消确认', { type: 'warning' })
  await cancelOrder(row.id)
  ElMessage.success('订单已取消')
  await loadOrders()
}

function bizTypeText(bizType) {
  return {
    FLIGHT: '机票',
    TRAIN: '火车票',
    HOTEL: '酒店',
    TOUR: '旅游'
  }[bizType] || bizType
}

function statusText(status) {
  return {
    10: '待支付',
    20: '待出行',
    30: '已完成',
    40: '已取消'
  }[status] || '未知状态'
}

function statusTagType(status) {
  return {
    10: 'warning',
    20: 'primary',
    30: 'success',
    40: 'info'
  }[status] || 'info'
}

onMounted(loadOrders)
</script>

<style scoped>
.order-page {
  display: grid;
  gap: 24px;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-select {
  width: 180px;
}

.order-table-wrap {
  overflow-x: auto;
}

.price-text {
  color: #d9480f;
  font-weight: 700;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
