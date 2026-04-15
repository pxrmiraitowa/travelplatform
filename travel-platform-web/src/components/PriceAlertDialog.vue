<template>
  <el-dialog v-model="visible" title="创建价格提醒" width="420px" @closed="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="产品类型">
        <el-input :model-value="typeLabel" disabled />
      </el-form-item>
      <el-form-item label="目标价格" prop="targetPrice">
        <el-input-number v-model="form.targetPrice" :min="1" :precision="2" :step="10" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="100" show-word-limit placeholder="例如：降到预算内时提醒我" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">保存提醒</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createPriceAlert } from '@/api/priceAlert'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  productType: {
    type: String,
    required: true
  },
  productId: {
    type: [String, Number],
    required: true
  },
  defaultTargetPrice: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref()
const submitting = ref(false)
const form = reactive({
  targetPrice: 0,
  remark: ''
})

const rules = {
  targetPrice: [{ required: true, message: '请输入目标价格', trigger: 'blur' }]
}

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const typeLabelMap = {
  HOTEL: '酒店',
  FLIGHT: '航班',
  TOUR: '旅游产品'
}

const typeLabel = computed(() => typeLabelMap[props.productType] || props.productType)

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      form.targetPrice = props.defaultTargetPrice || 0
      form.remark = ''
    }
  }
)

function resetForm() {
  form.targetPrice = props.defaultTargetPrice || 0
  form.remark = ''
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await createPriceAlert({
      productType: props.productType,
      productId: Number(props.productId),
      targetPrice: form.targetPrice,
      remark: form.remark
    })
    ElMessage.success('价格提醒已创建')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>
