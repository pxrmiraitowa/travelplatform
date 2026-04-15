<template>
  <div class="detail-page" v-loading="loading">
    <SectionCard title="车次详情" description="查看车次时刻、历时以及不同座位类型的价格与余票。">
      <div v-if="train" class="detail-grid">
        <div class="hero-row">
          <div>
            <div class="route-title">{{ train.departureCity }} -> {{ train.arrivalCity }}</div>
            <div class="meta-text">{{ train.trainType }} | {{ train.trainNo }} | 历时 {{ formatDuration(train.durationMinutes) }}</div>
          </div>
          <el-button type="primary" :disabled="!hasAvailableSeat" @click="goBooking">立即购票</el-button>
        </div>

        <div class="timeline-card">
          <div>
            <div class="time-text">{{ formatDateTime(train.departureTime, 'HH:mm') }}</div>
            <div>{{ train.departureStation }}</div>
            <div class="meta-text">{{ formatDateTime(train.departureTime, 'YYYY-MM-DD') }}</div>
          </div>
          <div class="timeline-middle">{{ formatDuration(train.durationMinutes) }}</div>
          <div class="timeline-right">
            <div class="time-text">{{ formatDateTime(train.arrivalTime, 'HH:mm') }}</div>
            <div>{{ train.arrivalStation }}</div>
            <div class="meta-text">{{ formatDateTime(train.arrivalTime, 'YYYY-MM-DD') }}</div>
          </div>
        </div>

        <el-table :data="train.seatOptions || []" border>
          <el-table-column prop="seatType" label="座位类型" min-width="120" />
          <el-table-column label="价格" min-width="120">
            <template #default="{ row }">
              <span class="price-text">{{ row.price && Number(row.price) > 0 ? `¥ ${Number(row.price).toFixed(2)}` : '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="余票" min-width="100">
            <template #default="{ row }">
              <el-tag :type="row.available ? 'success' : 'info'">{{ row.available ? `余票${row.stock}` : '不可购' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </SectionCard>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import SectionCard from '@/components/SectionCard.vue'
import { getTrainDetail } from '@/api/train'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const train = ref(null)

const hasAvailableSeat = computed(() => (train.value?.seatOptions || []).some(item => item.available))

async function loadTrainDetail() {
  loading.value = true
  try {
    const response = await getTrainDetail(route.params.id)
    train.value = response.data
  } finally {
    loading.value = false
  }
}

function goBooking() {
  router.push({ name: 'train-booking', params: { id: route.params.id } })
}

function formatDateTime(value, pattern = 'YYYY-MM-DD HH:mm') {
  return value ? dayjs(value).format(pattern) : '--'
}

function formatDuration(minutes) {
  if (!minutes && minutes !== 0) {
    return '--'
  }
  return `${Math.floor(minutes / 60)}时${minutes % 60}分`
}

onMounted(() => {
  loadTrainDetail()
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

.price-text {
  color: #d9480f;
  font-weight: 700;
}
</style>
