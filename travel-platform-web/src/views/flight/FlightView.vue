<template>
  <div class="flight-page">
    <SectionCard title="航班搜索" description="支持按出发地、目的地、日期、价格区间和起飞时间筛选模拟航班数据。">
      <el-form :model="searchForm" label-width="90px" class="search-form">
        <el-row :gutter="16">
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="出发地">
              <el-input v-model="searchForm.departureCity" clearable placeholder="例如：上海" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="目的地">
              <el-input v-model="searchForm.arrivalCity" clearable placeholder="例如：北京" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="出发日期">
              <el-date-picker v-model="searchForm.departureDate" type="date" value-format="YYYY-MM-DD" placeholder="选择出发日期" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="最低价格">
              <el-input-number v-model="searchForm.minPrice" :min="0" :precision="0" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="最高价格">
              <el-input-number v-model="searchForm.maxPrice" :min="0" :precision="0" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="起飞时间">
              <el-time-picker
                v-model="searchForm.departureTimeRange"
                is-range
                value-format="HH:mm"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                class="full-width"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="search-actions">
          <el-button type="primary" @click="handleSearch">搜索航班</el-button>
          <el-button @click="handleReset">重置筛选</el-button>
        </div>
      </el-form>
    </SectionCard>

    <SectionCard :title="`航班列表（共 ${pagination.total} 条）`" description="点击详情可查看完整航班信息，点击预订将进入下单页面。">
      <el-table :data="flights" border stripe v-loading="loading">
        <el-table-column prop="airlineName" label="航司" min-width="140" />
        <el-table-column prop="flightNo" label="航班号" min-width="110" />
        <el-table-column label="航线" min-width="200">
          <template #default="{ row }">
            <div>{{ row.departureCity }} -> {{ row.arrivalCity }}</div>
            <div class="sub-text">{{ row.departureAirport }} / {{ row.arrivalAirport }}</div>
          </template>
        </el-table-column>
        <el-table-column label="起降时间" min-width="180">
          <template #default="{ row }">
            <div>{{ formatDateTime(row.departureTime, 'MM-DD HH:mm') }}</div>
            <div class="sub-text">{{ formatDateTime(row.arrivalTime, 'MM-DD HH:mm') }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="cabinClass" label="舱位" min-width="100" />
        <el-table-column label="余票" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.stock > 0 ? 'success' : 'danger'">{{ row.stock > 0 ? `余票${row.stock}` : '售罄' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" min-width="110">
          <template #default="{ row }">
            <span class="price-text">¥ {{ Number(row.price).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">查看详情</el-button>
            <el-button link type="success" :disabled="row.stock <= 0" @click="goBooking(row.id)">立即预订</el-button>
          </template>
        </el-table-column>
      </el-table>

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
import dayjs from 'dayjs'
import SectionCard from '@/components/SectionCard.vue'
import { searchFlights } from '@/api/flight'

const router = useRouter()
const loading = ref(false)
const flights = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const defaultSearchForm = () => ({
  departureCity: '',
  arrivalCity: '',
  departureDate: '',
  minPrice: null,
  maxPrice: null,
  departureTimeRange: []
})

const searchForm = reactive(defaultSearchForm())

function buildQueryParams() {
  return {
    departureCity: searchForm.departureCity || undefined,
    arrivalCity: searchForm.arrivalCity || undefined,
    departureDate: searchForm.departureDate || undefined,
    minPrice: searchForm.minPrice ?? undefined,
    maxPrice: searchForm.maxPrice ?? undefined,
    departureStartTime: searchForm.departureTimeRange?.[0] || undefined,
    departureEndTime: searchForm.departureTimeRange?.[1] || undefined,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
}

async function loadFlights() {
  loading.value = true
  try {
    const response = await searchFlights(buildQueryParams())
    flights.value = response.data.records
    pagination.total = response.data.total
    pagination.pageNum = response.data.pageNum
    pagination.pageSize = response.data.pageSize
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadFlights()
}

function handleReset() {
  Object.assign(searchForm, defaultSearchForm())
  pagination.pageNum = 1
  loadFlights()
}

function handlePageChange(page) {
  pagination.pageNum = page
  loadFlights()
}

function goDetail(id) {
  router.push({ name: 'flight-detail', params: { id } })
}

function goBooking(id) {
  router.push({ name: 'flight-booking', params: { id } })
}

function formatDateTime(value, pattern = 'YYYY-MM-DD HH:mm') {
  return value ? dayjs(value).format(pattern) : '--'
}

onMounted(() => {
  loadFlights()
})
</script>

<style scoped>
.flight-page {
  display: grid;
  gap: 24px;
}

.search-form :deep(.el-date-editor),
.search-form :deep(.el-input-number),
.search-form :deep(.el-time-editor) {
  width: 100%;
}

.full-width {
  width: 100%;
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.sub-text {
  color: #7a869a;
  font-size: 12px;
}

.price-text {
  color: #d9480f;
  font-size: 18px;
  font-weight: 700;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
