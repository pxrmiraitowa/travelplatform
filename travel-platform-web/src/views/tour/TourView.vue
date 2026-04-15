<template>
  <div class="tour-page">
    <SectionCard title="旅游度假产品" description="浏览由平台维护的旅游度假产品，支持按目的地快速筛选。">
      <div class="toolbar">
        <el-input v-model="filters.destination" clearable placeholder="输入目的地，例如：三亚" class="search-input" />
        <el-button type="primary" @click="handleSearch">搜索产品</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <div v-loading="loading" class="tour-grid">
        <div v-for="item in tours" :key="item.id" class="tour-card">
          <div class="tour-top">
            <div>
              <h3>{{ item.packageName }}</h3>
              <div class="meta-text">{{ item.destination }} | {{ item.departureCity || '全国出发' }} | {{ item.days }}天</div>
            </div>
            <div class="price-box">¥ {{ Number(item.price).toFixed(2) }}</div>
          </div>
          <p class="desc-text">{{ item.description }}</p>
          <div class="date-tags">
            <el-tag v-for="date in item.travelDateOptions" :key="date" effect="plain">{{ date }}</el-tag>
          </div>
          <div class="footer-row">
            <span class="meta-text">库存：{{ item.stock }}</span>
            <div class="action-row">
              <el-button link type="primary" @click="goDetail(item.id)">查看详情</el-button>
              <el-button type="success" :disabled="item.stock <= 0" @click="goBooking(item.id)">立即下单</el-button>
            </div>
          </div>
        </div>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import SectionCard from '@/components/SectionCard.vue'
import { getTourList } from '@/api/tour'

const router = useRouter()
const loading = ref(false)
const tours = ref([])
const filters = reactive({
  destination: ''
})
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

async function loadTours() {
  loading.value = true
  try {
    const response = await getTourList({
      destination: filters.destination || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tours.value = response.data.records
    pagination.total = response.data.total
    pagination.pageNum = response.data.pageNum
    pagination.pageSize = response.data.pageSize
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadTours()
}

function handleReset() {
  filters.destination = ''
  pagination.pageNum = 1
  loadTours()
}

function handlePageChange(page) {
  pagination.pageNum = page
  loadTours()
}

function goDetail(id) {
  router.push({ name: 'tour-detail', params: { id } })
}

function goBooking(id) {
  router.push({ name: 'tour-booking', params: { id } })
}

onMounted(() => {
  loadTours()
})
</script>

<style scoped>
.tour-page {
  display: grid;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.tour-grid {
  display: grid;
  gap: 16px;
}

.tour-card {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  padding: 20px;
  background: #fff;
}

.tour-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.tour-top h3 {
  margin: 0;
  font-size: 22px;
}

.meta-text {
  color: #7a869a;
}

.price-box {
  color: #d9480f;
  font-size: 24px;
  font-weight: 700;
}

.desc-text {
  color: #475569;
}

.date-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.footer-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.action-row {
  display: flex;
  gap: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
