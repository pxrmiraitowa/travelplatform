<template>
  <div class="hotel-page">
    <SectionCard title="酒店搜索" description="按城市、入住日期和离店日期搜索可预订酒店，房型与价格来自数据库模拟数据。">
      <el-form :model="searchForm" label-width="90px" class="search-form">
        <el-row :gutter="16">
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="城市">
              <el-input v-model="searchForm.city" clearable placeholder="例如：上海" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="入住日期">
              <el-date-picker v-model="searchForm.checkInDate" type="date" value-format="YYYY-MM-DD" placeholder="选择入住日期" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="离店日期">
              <el-date-picker v-model="searchForm.checkOutDate" type="date" value-format="YYYY-MM-DD" placeholder="选择离店日期" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="search-actions">
          <el-button type="primary" @click="handleSearch">搜索酒店</el-button>
          <el-button @click="handleReset">重置筛选</el-button>
        </div>
      </el-form>
    </SectionCard>

    <SectionCard :title="`酒店列表（共 ${pagination.total} 条）`" description="点击详情查看酒店信息和房型价格，点击预订可直接选择房型下单。">
      <div v-loading="loading" class="hotel-list">
        <div v-for="item in hotels" :key="item.id" class="hotel-card">
          <div class="hotel-main">
            <div class="hotel-head">
              <div>
                <h3>{{ item.hotelName }}</h3>
                <div class="meta-text">{{ item.city }} {{ item.district || '' }} | {{ '★'.repeat(item.starLevel || 0) }}</div>
              </div>
              <div class="price-block">
                <span>¥ {{ Number(item.minPrice || 0).toFixed(2) }}</span>
                <small>起 / 晚</small>
              </div>
            </div>
            <p class="address-text">{{ item.address }}</p>
            <p class="desc-text">{{ item.description }}</p>
            <div class="footer-row">
              <div class="meta-text">可订房量：{{ item.availableRoomCount }}</div>
              <div class="action-row">
                <el-button link type="primary" @click="goDetail(item.id)">查看详情</el-button>
                <el-button type="success" @click="goBooking(item.id)">立即预订</el-button>
              </div>
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
import { searchHotels } from '@/api/hotel'

const router = useRouter()
const loading = ref(false)
const hotels = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const defaultSearchForm = () => ({
  city: '',
  checkInDate: '',
  checkOutDate: ''
})

const searchForm = reactive(defaultSearchForm())

function buildQueryParams() {
  return {
    city: searchForm.city || undefined,
    checkInDate: searchForm.checkInDate || undefined,
    checkOutDate: searchForm.checkOutDate || undefined,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
}

async function loadHotels() {
  loading.value = true
  try {
    const response = await searchHotels(buildQueryParams())
    hotels.value = response.data.records
    pagination.total = response.data.total
    pagination.pageNum = response.data.pageNum
    pagination.pageSize = response.data.pageSize
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadHotels()
}

function handleReset() {
  Object.assign(searchForm, defaultSearchForm())
  pagination.pageNum = 1
  loadHotels()
}

function handlePageChange(page) {
  pagination.pageNum = page
  loadHotels()
}

function goDetail(id) {
  router.push({ name: 'hotel-detail', params: { id } })
}

function goBooking(id) {
  router.push({ name: 'hotel-booking', params: { id }, query: { checkInDate: searchForm.checkInDate || '', checkOutDate: searchForm.checkOutDate || '' } })
}

onMounted(() => {
  loadHotels()
})
</script>

<style scoped>
.hotel-page {
  display: grid;
  gap: 24px;
}

.search-form :deep(.el-date-editor) {
  width: 100%;
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.hotel-list {
  display: grid;
  gap: 16px;
}

.hotel-card {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  padding: 20px;
  background: #fff;
}

.hotel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.hotel-head h3 {
  margin: 0;
  font-size: 22px;
}

.meta-text {
  color: #7a869a;
}

.price-block {
  text-align: right;
  color: #d9480f;
  font-size: 24px;
  font-weight: 700;
}

.price-block small {
  display: block;
  font-size: 12px;
}

.address-text,
.desc-text {
  color: #475569;
}

.footer-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
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
