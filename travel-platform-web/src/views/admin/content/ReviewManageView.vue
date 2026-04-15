<template>
  <div class="admin-page">
    <section class="admin-card">
      <div class="admin-section__head">
        <div>
          <h2>评价管理</h2>
          <p>查看订单评价内容，并对不合规评价执行下架处理。</p>
        </div>
      </div>

      <div class="admin-toolbar">
        <el-input v-model="query.keyword" placeholder="评价内容关键词" clearable class="admin-filter" />
        <el-select v-model="query.bizType" clearable placeholder="业务类型" class="admin-filter">
          <el-option label="机票" value="FLIGHT" />
          <el-option label="火车票" value="TRAIN" />
          <el-option label="酒店" value="HOTEL" />
          <el-option label="旅游" value="TOUR" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" class="admin-filter">
          <el-option label="正常" :value="1" />
          <el-option label="已下架" :value="2" />
        </el-select>
        <el-button type="primary" @click="loadReviews">查询</el-button>
      </div>

      <el-table :data="records" border v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="authorNickname" label="用户" min-width="120" />
        <el-table-column prop="bizType" label="类型" width="100" />
        <el-table-column prop="rating" label="评分" width="80" />
        <el-table-column prop="content" label="评价内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '已下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" link type="danger" @click="handleDelete(row.id)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { deleteAdminReview, getAdminReviews } from '@/api/admin'

const loading = ref(false)
const records = ref([])
const query = reactive({
  keyword: '',
  bizType: '',
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

async function loadReviews() {
  loading.value = true
  try {
    const response = await getAdminReviews(query)
    records.value = response.data.records || []
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  await deleteAdminReview(id)
  ElMessage.success('评价已下架')
  loadReviews()
}

onMounted(loadReviews)
</script>
