<template>
  <div class="order-detail-page">
    <SectionCard title="订单详情" description="统一展示订单基础信息、业务详情和评价信息。">
      <template #extra>
        <el-space wrap>
          <el-button @click="router.push('/orders')">返回订单列表</el-button>
          <el-button v-if="orderDetail && [10, 20].includes(orderDetail.orderStatus)" type="danger" @click="handleCancel">取消订单</el-button>
          <el-button v-if="orderDetail && orderDetail.orderStatus === 30 && !orderDetail.reviewed" type="warning" @click="reviewDialogVisible = true">去评价</el-button>
        </el-space>
      </template>

      <el-skeleton v-if="loading" :rows="8" animated />
      <template v-else-if="orderDetail">
        <OrderDetailPanel :order="orderDetail" />

        <div class="review-panel">
          <h3>订单评价</h3>
          <el-empty v-if="!orderDetail.reviewInfo" description="当前订单暂未评价" />
          <div v-else class="review-card">
            <el-rate :model-value="orderDetail.reviewInfo.rating" disabled />
            <p>{{ orderDetail.reviewInfo.content }}</p>
          </div>
        </div>
      </template>
      <el-empty v-else description="未找到订单信息" />
    </SectionCard>

    <ReviewDialog v-model="reviewDialogVisible" :order="orderDetail" @success="loadOrderDetail" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard.vue'
import OrderDetailPanel from '@/components/order/OrderDetailPanel.vue'
import ReviewDialog from '@/components/review/ReviewDialog.vue'
import { cancelOrder, getOrderDetail } from '@/api/order'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const orderDetail = ref(null)
const reviewDialogVisible = ref(false)

async function loadOrderDetail() {
  loading.value = true
  try {
    const response = await getOrderDetail(route.params.id)
    orderDetail.value = response.data
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  if (!orderDetail.value) {
    return
  }
  await ElMessageBox.confirm(`确认取消订单“${orderDetail.value.orderNo}”吗？`, '取消确认', { type: 'warning' })
  await cancelOrder(orderDetail.value.id)
  ElMessage.success('订单已取消')
  await loadOrderDetail()
}

onMounted(loadOrderDetail)
</script>

<style scoped>
.order-detail-page {
  display: grid;
  gap: 24px;
}

.review-panel {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.review-card {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
}

.review-card p {
  margin: 12px 0 0;
  color: #475569;
  line-height: 1.8;
}
</style>
