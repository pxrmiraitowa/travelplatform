<template>
  <div class="detail-page" v-loading="loading">
    <SectionCard title="航班详情" description="查看航班信息、价格对比结果、低价提示和模拟优惠券。">
      <div v-if="flight" class="detail-grid">
        <div class="hero-row">
          <div>
            <div class="route-title">{{ flight.departureCity }} -> {{ flight.arrivalCity }}</div>
            <div class="meta-text">{{ flight.airlineName }} | {{ flight.flightNo }} | {{ flight.cabinClass }}</div>
          </div>
          <div class="hero-actions">
            <div class="price-block">￥{{ Number(flight.price).toFixed(2) }}</div>
            <el-button type="primary" :disabled="flight.stock <= 0" @click="goBooking">立即预订</el-button>
          </div>
        </div>

        <div class="timeline-card">
          <div>
            <div class="time-text">{{ formatDateTime(flight.departureTime, 'HH:mm') }}</div>
            <div>{{ flight.departureAirport }}</div>
            <div class="meta-text">{{ formatDateTime(flight.departureTime, 'YYYY-MM-DD') }}</div>
          </div>
          <div class="timeline-middle">直飞</div>
          <div class="timeline-right">
            <div class="time-text">{{ formatDateTime(flight.arrivalTime, 'HH:mm') }}</div>
            <div>{{ flight.arrivalAirport }}</div>
            <div class="meta-text">{{ formatDateTime(flight.arrivalTime, 'YYYY-MM-DD') }}</div>
          </div>
        </div>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="航司">{{ flight.airlineName }}</el-descriptions-item>
          <el-descriptions-item label="航班号">{{ flight.flightNo }}</el-descriptions-item>
          <el-descriptions-item label="舱位">{{ flight.cabinClass }}</el-descriptions-item>
          <el-descriptions-item label="余票">{{ flight.stock }}</el-descriptions-item>
          <el-descriptions-item label="行李规则">{{ flight.baggagePolicy }}</el-descriptions-item>
          <el-descriptions-item label="退改签">{{ flight.refundPolicy }}</el-descriptions-item>
        </el-descriptions>

        <PriceComparePanel :loading="compareLoading" :data="compareData" @create-alert="openAlertDialog" />
      </div>
    </SectionCard>

    <PriceAlertDialog
      v-model="alertDialogVisible"
      product-type="FLIGHT"
      :product-id="route.params.id"
      :default-target-price="defaultTargetPrice"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import SectionCard from '@/components/SectionCard.vue'
import PriceComparePanel from '@/components/PriceComparePanel.vue'
import PriceAlertDialog from '@/components/PriceAlertDialog.vue'
import { getFlightDetail } from '@/api/flight'
import { getFlightPriceCompare } from '@/api/price'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const compareLoading = ref(false)
const flight = ref(null)
const compareData = ref(null)
const alertDialogVisible = ref(false)
const defaultTargetPrice = ref(0)

async function loadFlightDetail() {
  loading.value = true
  try {
    const response = await getFlightDetail(route.params.id)
    flight.value = response.data
  } finally {
    loading.value = false
  }
}

async function loadCompareData() {
  compareLoading.value = true
  try {
    const response = await getFlightPriceCompare(route.params.id)
    compareData.value = response.data
    defaultTargetPrice.value = Number(response.data.lowestPrice || response.data.currentPrice || 0)
  } finally {
    compareLoading.value = false
  }
}

function goBooking() {
  router.push({ name: 'flight-booking', params: { id: route.params.id } })
}

function openAlertDialog() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再创建价格提醒')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  alertDialogVisible.value = true
}

function formatDateTime(value, pattern = 'YYYY-MM-DD HH:mm') {
  return value ? dayjs(value).format(pattern) : '--'
}

onMounted(() => {
  loadFlightDetail()
  loadCompareData()
})
</script>

<style scoped>
.detail-page {
  display: grid;
}

.detail-grid {
  display: grid;
  gap: 20px;
}

.hero-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.route-title {
  font-size: 28px;
  font-weight: 700;
}

.meta-text {
  color: #7a869a;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.price-block {
  color: #d9480f;
  font-size: 24px;
  font-weight: 700;
}

.timeline-card {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 16px;
  align-items: center;
  padding: 20px;
  background: #f8fafc;
  border-radius: 16px;
}

.timeline-middle {
  color: #3b82f6;
  font-weight: 600;
}

.timeline-right {
  text-align: right;
}

.time-text {
  font-size: 24px;
  font-weight: 700;
}
</style>
