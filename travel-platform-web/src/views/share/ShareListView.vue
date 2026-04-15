<template>
  <div class="share-page">
    <SectionCard title="旅行分享" description="看看其他用户的游记、心得和实拍图片，也可以发布自己的旅行故事。">
      <template #extra>
        <el-button type="primary" @click="router.push(userStore.isLoggedIn ? '/shares/create' : '/login')">发布分享</el-button>
      </template>

      <el-row :gutter="20" v-loading="loading">
        <el-col v-for="item in shares" :key="item.id" :xs="24" :sm="12" :lg="8">
          <div class="share-card" @click="router.push(`/shares/${item.id}`)">
            <img :src="item.coverImage" class="cover" alt="share-cover" />
            <div class="card-body">
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary }}</p>
              <div class="meta">
                <span>{{ item.authorNickname }}</span>
                <span>{{ formatDate(item.createTime) }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && !shares.length" description="还没有分享内容" />

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
import { getShareList } from '@/api/share'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const shares = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 9,
  total: 0
})

async function loadShares() {
  loading.value = true
  try {
    const response = await getShareList(pagination)
    shares.value = response.data.records || []
    pagination.total = response.data.total || 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  pagination.pageNum = page
  loadShares()
}

function formatDate(value) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '--'
}

onMounted(loadShares)
</script>

<style scoped>
.share-page {
  display: grid;
  gap: 24px;
}

.share-card {
  overflow: hidden;
  margin-bottom: 20px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(15, 95, 168, 0.08);
  cursor: pointer;
}

.cover {
  width: 100%;
  height: 220px;
  object-fit: cover;
  display: block;
}

.card-body {
  padding: 18px;
}

.card-body h3 {
  margin: 0 0 10px;
  color: #12314d;
}

.card-body p {
  min-height: 48px;
  color: #66758d;
}

.meta {
  display: flex;
  justify-content: space-between;
  color: #7a869a;
  font-size: 13px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
}
</style>
