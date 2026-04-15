<template>
  <el-dialog :model-value="modelValue" title="订单评价" width="520px" @close="emit('update:modelValue', false)">
    <el-form label-width="84px">
      <el-form-item label="订单号">
        <span>{{ order?.orderNo || '--' }}</span>
      </el-form-item>
      <el-form-item label="评分">
        <el-rate v-model="form.rating" />
      </el-form-item>
      <el-form-item label="评价内容">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="5"
          maxlength="500"
          show-word-limit
          placeholder="分享这次出行体验、服务感受或注意事项"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交评价</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createReview } from '@/api/review'

const props = defineProps({
  modelValue: Boolean,
  order: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const form = reactive({
  rating: 5,
  content: ''
})

const submitting = ref(false)

watch(() => props.modelValue, (visible) => {
  if (visible) {
    form.rating = 5
    form.content = ''
  }
})

async function handleSubmit() {
  if (!props.order?.id) {
    return
  }
  if (!form.rating) {
    ElMessage.warning('请选择评分')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  submitting.value = true
  try {
    await createReview({
      orderId: props.order.id,
      rating: form.rating,
      content: form.content.trim()
    })
    ElMessage.success('评价提交成功')
    emit('success')
    emit('update:modelValue', false)
  } finally {
    submitting.value = false
  }
}
</script>
