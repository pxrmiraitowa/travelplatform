<template>
  <div class="detail-page" v-loading="loading">
    <SectionCard title="旅游产品详情" description="查看产品信息、可选出行日期，以及同类产品价格对比和优惠提醒。">
      <div v-if="tour" class="detail-grid">
        <div class="hero-row">
          <div>
            <div class="title-line">{{ tour.packageName }}</div>
            <div class="meta-text">{{ tour.destination }} | {{ tour.departureCity || '全国出发' }} | {{ tour.days }}天</div>
            <div class="meta-text">库存：{{ tour.stock }}</div>
          </div>
          <div class="hero-side">
            <div class="price-text">￥{{ Number(tour.price).toFixed(2) }}</div>
            <el-button type="primary" :disabled="tour.stock <= 0" @click="goBooking">立即下单</el-button>
          </div>
        </div>

        <div class="desc-box">{{ tour.description }}</div>

        <div class="date-panel">
          <h4>可选出行日期</h4>
          <div class="date-tags">
            <el-tag
              v-for="date in tour.travelDateOptions || []"
              :key="date"
              :type="selectedDate === date ? 'success' : 'info'"
              effect="plain"
              class="date-tag"
              @click="selectedDate = date"
            >
              {{ date }}
            </el-tag>
          </div>
          <div class="date-tip">当前已选：{{ selectedDate || '请选择日期' }}</div>
        </div>

        <PriceComparePanel :loading="compareLoading" :data="compareData" @create-alert="openAlertDialog" />
      </div>
    </SectionCard>

    <PriceAlertDialog
      v-model="alertDialogVisible"
      product-type="TOUR"
      :product-id="route.params.id"
      :default-target-price="defaultTargetPrice"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SectionCard from '@/components/SectionCard.vue'
import PriceComparePanel from '@/components/PriceComparePanel.vue'
import PriceAlertDialog from '@/components/PriceAlertDialog.vue'
import { getTourDetail } from '@/api/tour'
import { getTourPriceCompare } from '@/api/price'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const compareLoading = ref(false)
const tour = ref(null)
const selectedDate = ref('')
const compareData = ref(null)
const alertDialogVisible = ref(false)
const defaultTargetPrice = ref(0)

async function loadTourDetail() {
  loading.value = true
  try {
    const response = await getTourDetail(route.params.id)
    tour.value = response.data
    selectedDate.value = response.data.travelDateOptions?.[0] || ''
  } finally {
    loading.value = false
  }
}

async function loadCompareData() {
  compareLoading.value = true
  try {
    const response = await getTourPriceCompare(route.params.id)
    compareData.value = response.data
    defaultTargetPrice.value = Number(response.data.lowestPrice || response.data.currentPrice || 0)
  } finally {
    compareLoading.value = false
  }
}

function goBooking() {
  router.push({
    name: 'tour-booking',
    params: { id: route.params.id },
    query: { travelDate: selectedDate.value || '' }
  })
}

function openAlertDialog() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再创建价格提醒')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  alertDialogVisible.value = true
}

onMounted(() => {
  loadTourDetail()
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

.title-line {
  font-size: 28px;
  font-weight: 700;
}

.meta-text {
  color: #7a869a;
}

.hero-side {
  text-align: right;
}

.price-text {
  color: #d9480f;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 10px;
}

.desc-box,
.date-panel {
  padding: 18px;
  border-radius: 14px;
  background: #f8fafc;
}

.date-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.date-tag {
  cursor: pointer;
}

.date-tip {
  margin-top: 12px;
  color: #7a869a;
  font-size: 13px;
}
</style>
