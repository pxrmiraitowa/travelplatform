<template>
  <div class="admin-page">
    <section class="admin-card">
      <div class="admin-section__head">
        <div>
          <h2>分享管理</h2>
          <p>查看用户发布的旅行分享，并对不合规内容执行下架处理。</p>
        </div>
      </div>

      <div class="admin-toolbar">
        <el-input v-model="query.keyword" placeholder="标题 / 摘要关键词" clearable class="admin-filter" />
        <el-select v-model="query.status" clearable placeholder="状态" class="admin-filter">
          <el-option label="正常" :value="1" />
          <el-option label="已下架" :value="2" />
        </el-select>
        <el-button type="primary" @click="loadShares">查询</el-button>
      </div>

      <el-table :data="records" border v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="240" />
        <el-table-column prop="authorNickname" label="作者" min-width="120" />
        <el-table-column prop="viewCount" label="浏览量" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '已下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" min-width="180" />
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
import { deleteAdminShare, getAdminShares } from '@/api/admin'

const loading = ref(false)
const records = ref([])
const query = reactive({
  keyword: '',
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

async function loadShares() {
  loading.value = true
  try {
    const response = await getAdminShares(query)
    records.value = response.data.records || []
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  await deleteAdminShare(id)
  ElMessage.success('分享已下架')
  loadShares()
}

onMounted(loadShares)
</script>
