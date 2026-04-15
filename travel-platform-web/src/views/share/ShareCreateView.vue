<template>
  <div class="share-create-page">
    <SectionCard title="发布旅行分享" description="上传图片、填写标题和内容，把这次旅途的风景和心得记录下来。">
      <el-form label-width="88px">
        <el-form-item label="分享标题">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：杭州三天两晚自由行攻略" />
        </el-form-item>
        <el-form-item label="分享摘要">
          <el-input v-model="form.summary" maxlength="255" show-word-limit placeholder="一句话概括本次旅行亮点" />
        </el-form-item>
        <el-form-item label="分享内容">
          <el-input v-model="form.content" type="textarea" :rows="10" maxlength="5000" show-word-limit />
        </el-form-item>
        <el-form-item label="分享图片">
          <el-upload
            :http-request="handleUpload"
            list-type="picture-card"
            :file-list="fileList"
            :limit="9"
            multiple
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button @click="router.push('/shares')">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">发布分享</el-button>
        </el-form-item>
      </el-form>
    </SectionCard>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import SectionCard from '@/components/SectionCard.vue'
import { createShare, uploadShareImage } from '@/api/share'

const router = useRouter()
const submitting = ref(false)
const fileList = ref([])
const form = reactive({
  title: '',
  summary: '',
  content: '',
  imageUrls: []
})

async function handleUpload(option) {
  const response = await uploadShareImage(option.file)
  form.imageUrls.push(response.data.url)
  fileList.value = [
    ...fileList.value,
    {
      name: option.file.name,
      url: response.data.url
    }
  ]
  option.onSuccess(response)
}

async function handleSubmit() {
  if (!form.title.trim() || !form.summary.trim() || !form.content.trim()) {
    ElMessage.warning('请完整填写分享内容')
    return
  }
  if (!form.imageUrls.length) {
    ElMessage.warning('请至少上传一张图片')
    return
  }
  submitting.value = true
  try {
    const response = await createShare({
      title: form.title.trim(),
      summary: form.summary.trim(),
      content: form.content.trim(),
      imageUrls: form.imageUrls
    })
    ElMessage.success('分享发布成功')
    router.push(`/shares/${response.data.id}`)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.share-create-page {
  display: grid;
  gap: 24px;
}
</style>
