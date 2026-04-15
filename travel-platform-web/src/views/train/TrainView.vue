<template>
  <div class="train-page">
    <SectionCard title="车次搜索" description="支持按出发地、目的地、日期、车次类型和价格区间筛选模拟车次数据。">
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
              <el-date-picker v-model="searchForm.travelDate" type="date" value-format="YYYY-MM-DD" placeholder="选择乘车日期" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="车次类型">
              <el-select v-model="searchForm.trainType" clearable placeholder="全部车次">
                <el-option label="高铁" value="高铁" />
                <el-option label="动车" value="动车" />
                <el-option label="快速" value="快速" />
                <el-option label="直达" value="直达" />
              </el-select>
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
        </el-row>
        <div class="search-actions">
          <el-button type="primary" @click="handleSearch">搜索车次</el-button>
          <el-button @click="handleReset">重置筛选</el-button>
        </div>
      </el-form>
    </SectionCard>

    <SectionCard :title="`车次列表（共 ${pagination.total} 条）`" description="点击详情查看各座位价格与余票，点击购票进入下单页面。">
      <el-table :data="trains" border stripe v-loading="loading">
        <el-table-column prop="trainNo" label="车次" min-width="120" />
        <el-table-column prop="trainType" label="类型" min-width="100" />
        <el-table-column label="站点" min-width="220">
          <template #default="{ row }">
            <div>{{ row.departureCity }} -> {{ row.arrivalCity }}</div>
            <div class="sub-text">{{ row.departureStation }} / {{ row.arrivalStation }}</div>
          </template>
        </el-table-column>
        <el-table-column label="发到时间" min-width="180">
          <template #default="{ row }">
            <div>{{ formatDateTime(row.departureTime, 'MM-DD HH:mm') }}</div>
            <div class="sub-text">{{ formatDateTime(row.arrivalTime, 'MM-DD HH:mm') }}</div>
          </template>
        </el-table-column>
        <el-table-column label="历时" min-width="100">
          <template #default="{ row }">
            {{ formatDuration(row.durationMinutes) }}
          </template>
        </el-table-column>
        <el-table-column label="起售价" min-width="110">
          <template #default="{ row }">
            <span class="price-text">¥ {{ Number(row.minPrice || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="余票" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.totalStock > 0 ? 'success' : 'danger'">{{ row.totalStock > 0 ? `余票${row.totalStock}` : '售罄' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">查看详情</el-button>
            <el-button link type="success" :disabled="row.totalStock <= 0" @click="goBooking(row.id)">立即购票</el-button>
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
import { searchTrains } from '@/api/train'

const router = useRouter()
const loading = ref(false)
const trains = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const defaultSearchForm = () => ({
  departureCity: '',
  arrivalCity: '',
  travelDate: '',
  trainType: '',
  minPrice: null,
  maxPrice: null
})

const searchForm = reactive(defaultSearchForm())

function buildQueryParams() {
  return {
    departureCity: searchForm.departureCity || undefined,
    arrivalCity: searchForm.arrivalCity || undefined,
    travelDate: searchForm.travelDate || undefined,
    trainType: searchForm.trainType || undefined,
    minPrice: searchForm.minPrice ?? undefined,
    maxPrice: searchForm.maxPrice ?? undefined,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
}

async function loadTrains() {
  loading.value = true
  try {
    const response = await searchTrains(buildQueryParams())
    trains.value = response.data.records
    pagination.total = response.data.total
    pagination.pageNum = response.data.pageNum
    pagination.pageSize = response.data.pageSize
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadTrains()
}

function handleReset() {
  Object.assign(searchForm, defaultSearchForm())
  pagination.pageNum = 1
  loadTrains()
}

function handlePageChange(page) {
  pagination.pageNum = page
  loadTrains()
}

function goDetail(id) {
  router.push({ name: 'train-detail', params: { id } })
}

function goBooking(id) {
  router.push({ name: 'train-booking', params: { id } })
}

function formatDateTime(value, pattern = 'YYYY-MM-DD HH:mm') {
  return value ? dayjs(value).format(pattern) : '--'
}

function formatDuration(minutes) {
  if (!minutes && minutes !== 0) {
    return '--'
  }
  const hours = Math.floor(minutes / 60)
  const remain = minutes % 60
  return `${hours}时${remain}分`
}

onMounted(() => {
  loadTrains()
})
</script>

<style scoped>
.train-page {
  display: grid;
  gap: 24px;
}

.search-form :deep(.el-date-editor),
.search-form :deep(.el-input-number),
.search-form :deep(.el-select) {
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
