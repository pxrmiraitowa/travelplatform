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
          <div
            class="upload-drop-zone"
            :class="{ 'is-dragover': isDragover }"
            @dragenter.prevent="handleDragEnter"
            @dragover.prevent="handleDragOver"
            @dragleave.prevent="handleDragLeave"
            @drop.prevent="handleDrop"
          >
            <div class="drop-tip">拖拽图片到这里上传，也可以点击加号选择图片</div>
            <el-upload
              :http-request="handleUpload"
              list-type="picture-card"
              :file-list="fileList"
              :limit="9"
              :on-preview="handlePreview"
              :on-remove="handleRemove"
              multiple
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button @click="router.push('/shares')">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">发布分享</el-button>
        </el-form-item>
      </el-form>
    </SectionCard>
    <el-dialog v-model="previewVisible" width="720px" append-to-body>
      <img v-if="previewUrl" :src="previewUrl" class="preview-image" alt="share preview" />
    </el-dialog>
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
const isDragover = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')
let uploadUid = Date.now()
const form = reactive({
  title: '',
  summary: '',
  content: '',
  imageUrls: []
})

async function handleUpload(option) {
  try {
    const response = await uploadSingleFile(option.file)
    option.onSuccess(response)
  } catch (error) {
    option.onError(error)
  }
}

async function uploadSingleFile(rawFile) {
  const response = await uploadShareImage(rawFile)
  const uploadFile = {
    name: rawFile.name,
    uid: resolveFileUid(rawFile),
    status: 'success',
    url: response.data.url
  }
  fileList.value = [
    ...fileList.value.filter((file) => file.uid !== uploadFile.uid),
    uploadFile
  ]
  syncImageUrls()
  return response
}

function handleDragEnter() {
  isDragover.value = true
}

function handleDragOver() {
  isDragover.value = true
}

function handleDragLeave(event) {
  if (!event.currentTarget.contains(event.relatedTarget)) {
    isDragover.value = false
  }
}

async function handleDrop(event) {
  isDragover.value = false
  const files = Array.from(event.dataTransfer?.files || [])
    .filter((file) => file.type.startsWith('image/'))

  if (!files.length) {
    ElMessage.warning('请拖入图片文件')
    return
  }

  const remainCount = 9 - fileList.value.length
  if (remainCount <= 0) {
    ElMessage.warning('最多上传9张图片')
    return
  }

  const uploadFiles = files.slice(0, remainCount)
  if (files.length > remainCount) {
    ElMessage.warning(`最多上传9张图片，已自动忽略${files.length - remainCount}张`)
  }

  for (const file of uploadFiles) {
    try {
      await uploadSingleFile(file)
    } catch (error) {
      // The shared request interceptor has already shown the backend message.
    }
  }
}

function handlePreview(file) {
  const url = resolveFileUrl(file)
  if (!url) {
    return
  }
  previewUrl.value = url
  previewVisible.value = true
}

function handleRemove(file, uploadFiles) {
  const removedUrl = resolveFileUrl(file)
  fileList.value = uploadFiles
    .filter((item) => item.uid !== file.uid && resolveFileUrl(item) !== removedUrl)
    .map((item) => ({
      ...item,
      url: resolveFileUrl(item)
    }))
  syncImageUrls()
}

function syncImageUrls() {
  form.imageUrls = fileList.value
    .map(resolveFileUrl)
    .filter(Boolean)
}

function resolveFileUrl(file) {
  return file?.url || file?.response?.data?.url || ''
}

function resolveFileUid(file) {
  if (file.uid) {
    return file.uid
  }
  uploadUid += 1
  return uploadUid
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

.upload-drop-zone {
  width: 100%;
  padding: 14px;
  border: 1px dashed #c8d3e1;
  border-radius: 12px;
  background: #f8fbff;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.upload-drop-zone.is-dragover {
  border-color: #409eff;
  background: #ecf5ff;
}

.drop-tip {
  margin-bottom: 12px;
  color: #607086;
  font-size: 14px;
}

.preview-image {
  display: block;
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
}
</style>
