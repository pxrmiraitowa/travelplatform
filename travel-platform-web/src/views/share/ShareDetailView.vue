<template>
  <div class="share-detail-page">
    <SectionCard v-if="detail" :title="detail.title" :description="detail.summary">
      <template #extra>
        <el-button @click="router.push('/shares')">返回列表</el-button>
      </template>

      <div class="detail-meta">
        <span>作者：{{ detail.authorNickname }}</span>
        <span>发布时间：{{ formatDate(detail.createTime) }}</span>
        <span>浏览：{{ detail.viewCount }}</span>
      </div>

      <el-carousel v-if="detail.imageUrls?.length" height="420px" indicator-position="outside">
        <el-carousel-item v-for="image in detail.imageUrls" :key="image">
          <img :src="image" class="detail-image" alt="share-image" />
        </el-carousel-item>
      </el-carousel>

      <div class="content">{{ detail.content }}</div>
    </SectionCard>

    <el-skeleton v-else-if="loading" :rows="8" animated />
    <el-empty v-else description="分享内容不存在" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import SectionCard from '@/components/SectionCard.vue'
import { getShareDetail } from '@/api/share'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)

async function loadDetail() {
  loading.value = true
  try {
    const response = await getShareDetail(route.params.id)
    detail.value = response.data
  } finally {
    loading.value = false
  }
}

function formatDate(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--'
}

onMounted(loadDetail)
</script>

<style scoped>
.share-detail-page {
  display: grid;
  gap: 24px;
}

.detail-meta {
  display: flex;
  gap: 18px;
  margin-bottom: 18px;
  flex-wrap: wrap;
  color: #6b7280;
}

.detail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 16px;
}

.content {
  margin-top: 20px;
  white-space: pre-wrap;
  line-height: 1.9;
  color: #334155;
}
</style>
