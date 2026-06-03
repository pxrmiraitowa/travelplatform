<template>
  <div class="detail-page" v-loading="loading">
    <SectionCard title="酒店详情" description="查看酒店介绍、房型价格，以及同类酒店价格对比和优惠提醒。">
      <div v-if="hotel" class="detail-grid">
        <div v-if="heroImage" class="hero-image">
          <el-image :src="heroImage" fit="cover" :preview-src-list="galleryImages.length ? galleryImages : [heroImage]" preview-teleported />
        </div>

        <div class="hero-row">
          <div>
            <div class="title-line">{{ hotel.hotelName }}</div>
            <div class="meta-text">{{ hotel.city }} {{ hotel.district || '' }} | {{ '★'.repeat(hotel.starLevel || 0) }}</div>
            <div class="meta-text">{{ hotel.address }}</div>
          </div>
          <div class="hero-side">
            <div class="price-text">￥{{ Number(hotel.minPrice || 0).toFixed(2) }} 起</div>
            <div class="meta-text">入住 {{ hotel.checkInTime }} / 退房 {{ hotel.checkOutTime }}</div>
          </div>
        </div>

        <div class="desc-box">{{ hotel.description }}</div>

        <div v-if="galleryImages.length > 1" class="gallery-grid">
          <div v-for="image in galleryImages" :key="image" class="gallery-card">
            <el-image :src="image" fit="cover" :preview-src-list="galleryImages" preview-teleported />
          </div>
        </div>

        <el-table :data="hotel.roomList || []" border>
          <el-table-column prop="roomName" label="房型" min-width="150" />
          <el-table-column prop="bedType" label="床型" min-width="140" />
          <el-table-column prop="breakfast" label="早餐" min-width="100" />
          <el-table-column prop="roomArea" label="面积" min-width="90" />
          <el-table-column prop="guestCount" label="可住人数" min-width="100" />
          <el-table-column label="价格" min-width="120">
            <template #default="{ row }">
              <span class="price-text">￥{{ Number(row.price).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="库存" min-width="90">
            <template #default="{ row }">
              <el-tag :type="row.stock > 0 ? 'success' : 'danger'">{{ row.stock > 0 ? `余量${row.stock}` : '满房' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="cancelRule" label="取消规则" min-width="220" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :disabled="row.stock <= 0" @click="goBooking(row.id)">选择房型</el-button>
            </template>
          </el-table-column>
        </el-table>

        <PriceComparePanel :loading="compareLoading" :data="compareData" @create-alert="openAlertDialog" />
      </div>
    </SectionCard>

    <PriceAlertDialog
      v-model="alertDialogVisible"
      product-type="HOTEL"
      :product-id="route.params.id"
      :default-target-price="defaultTargetPrice"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SectionCard from '@/components/SectionCard.vue'
import PriceComparePanel from '@/components/PriceComparePanel.vue'
import PriceAlertDialog from '@/components/PriceAlertDialog.vue'
import { getHotelDetail } from '@/api/hotel'
import { getHotelPriceCompare } from '@/api/price'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const compareLoading = ref(false)
const hotel = ref(null)
const compareData = ref(null)
const alertDialogVisible = ref(false)
const defaultTargetPrice = ref(0)
const galleryImages = computed(() => hotel.value?.detailImages || [])
const heroImage = computed(() => hotel.value?.coverImage || galleryImages.value[0] || '')

async function loadHotelDetail() {
  loading.value = true
  try {
    const response = await getHotelDetail(route.params.id)
    hotel.value = response.data
  } finally {
    loading.value = false
  }
}

async function loadCompareData() {
  compareLoading.value = true
  try {
    const response = await getHotelPriceCompare(route.params.id)
    compareData.value = response.data
    defaultTargetPrice.value = Number(response.data.lowestPrice || response.data.currentPrice || 0)
  } finally {
    compareLoading.value = false
  }
}

function goBooking(roomId) {
  router.push({
    name: 'hotel-booking',
    params: { id: route.params.id },
    query: { roomId }
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
  loadHotelDetail()
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

.hero-image {
  width: 100%;
  border-radius: 18px;
  overflow: hidden;
  background: #e2e8f0;
}

.hero-image :deep(.el-image) {
  display: block;
  width: 100%;
}

.hero-image :deep(img) {
  display: block;
  width: 100%;
  height: 360px;
  object-fit: cover;
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
  font-weight: 700;
}

.desc-box {
  padding: 18px;
  border-radius: 14px;
  background: #f8fafc;
  color: #475569;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.gallery-card {
  border-radius: 16px;
  overflow: hidden;
  background: #e2e8f0;
}

.gallery-card :deep(.el-image) {
  display: block;
  width: 100%;
}

.gallery-card :deep(img) {
  display: block;
  width: 100%;
  height: 180px;
  object-fit: cover;
}

@media (max-width: 768px) {
  .hero-image :deep(img) {
    height: 240px;
  }
}
</style>
