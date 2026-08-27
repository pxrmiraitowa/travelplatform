<template>
  <div class="booking-page" v-loading="loading">
    <SectionCard title="火车票下单" description="请选择座位类型和乘车人，订单会进入统一订单中心。">
      <div v-if="train" class="booking-layout">
        <div class="train-summary">
          <div class="summary-title">{{ train.departureCity }} -> {{ train.arrivalCity }}</div>
          <div class="summary-line">{{ train.trainType }} | {{ train.trainNo }} | 历时 {{ formatDuration(train.durationMinutes) }}</div>
          <div class="summary-line">{{ formatDateTime(train.departureTime) }} - {{ formatDateTime(train.arrivalTime) }}</div>
          <div class="summary-line">{{ train.departureStation }} -> {{ train.arrivalStation }}</div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="booking-form">
          <el-form-item label="座位类型" prop="seatType">
            <el-radio-group v-model="form.seatType">
              <el-radio-button
                v-for="item in availableSeats"
                :key="item.seatType"
                :label="item.seatType"
              >
                {{ item.seatType }} ¥{{ Number(item.price).toFixed(2) }} / 余票{{ item.stock }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="乘车人" prop="contactId">
            <el-select v-model="form.contactId" placeholder="请选择常用联系人">
              <el-option
                v-for="item in contacts"
                :key="item.id"
                :label="`${item.name}（${item.phone}）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="可填写乘车备注" />
          </el-form-item>
          <div v-if="!contacts.length" class="contact-tip">
            当前没有常用联系人，请先前往个人中心维护乘车人信息。
            <el-button link type="primary" @click="router.push('/profile')">去维护联系人</el-button>
          </div>
          <div class="form-actions">
            <div class="submit-price">订单金额：{{ currentSeatPrice }}</div>
            <div class="form-buttons">
              <el-button @click="router.back()">返回</el-button>
              <el-button type="primary" :disabled="!contacts.length || !availableSeats.length" :loading="submitting" @click="submitOrder">提交订单</el-button>
            </div>
          </div>
        </el-form>
      </div>
    </SectionCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import SectionCard from '@/components/SectionCard.vue'
import { createTrainOrder } from '@/api/order'
import { getTrainDetail } from '@/api/train'
import { getUserContacts } from '@/api/userContact'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const train = ref(null)
const contacts = ref([])

const form = reactive({
  seatType: '',
  contactId: null,
  remark: ''
})

const rules = {
  seatType: [{ required: true, message: '请选择座位类型', trigger: 'change' }],
  contactId: [{ required: true, message: '请选择乘车人', trigger: 'change' }]
}

const availableSeats = computed(() => (train.value?.seatOptions || []).filter(item => item.available))
const currentSeatPrice = computed(() => {
  const seat = availableSeats.value.find(item => item.seatType === form.seatType)
  return seat ? `¥ ${Number(seat.price).toFixed(2)}` : '--'
})

async function loadData() {
  loading.value = true
  try {
    const [trainResponse, contactResponse] = await Promise.all([
      getTrainDetail(route.params.id),
      getUserContacts()
    ])
    train.value = trainResponse.data
    contacts.value = contactResponse.data
    const defaultSeat = availableSeats.value[0]
    const defaultContact = contacts.value.find(item => item.isDefault === 1) || contacts.value[0]
    form.seatType = defaultSeat?.seatType || ''
    form.contactId = defaultContact?.id ?? null
  } finally {
    loading.value = false
  }
}

async function submitOrder() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    const contact = contacts.value.find(item => item.id === form.contactId)
    const response = await createTrainOrder({
      productType: 'TRAIN',
      productId: Number(route.params.id),
      variantName: form.seatType,
      quantity: 1,
      travelDate: train.value.departureTime?.slice(0, 10),
      contactName: contact?.name,
      contactPhone: contact?.phone
    })
    ElMessage.success('火车票订单提交成功')
    router.push({ name: 'orders', query: { highlight: response.data.id } })
  } finally {
    submitting.value = false
  }
}

function formatDateTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--'
}

function formatDuration(minutes) {
  if (!minutes && minutes !== 0) {
    return '--'
  }
  return `${Math.floor(minutes / 60)}时${minutes % 60}分`
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.booking-page {
  display: grid;
}

.booking-layout {
  display: grid;
  gap: 24px;
}

.train-summary {
  padding: 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #ecfeff, #f8fafc);
}

.summary-title {
  font-size: 26px;
  font-weight: 700;
}

.summary-line {
  margin-top: 8px;
  color: #475569;
}

.booking-form :deep(.el-select) {
  width: 100%;
}

.booking-form :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.contact-tip {
  margin-bottom: 16px;
  color: #b45309;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.submit-price {
  color: #d9480f;
  font-size: 20px;
  font-weight: 700;
}

.form-buttons {
  display: flex;
  gap: 12px;
}
</style>
