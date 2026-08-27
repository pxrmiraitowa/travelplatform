<template>
  <div class="booking-page" v-loading="loading">
    <SectionCard title="机票下单" description="选择联系人后提交订单，订单金额以服务端商品快照为准。">
      <div v-if="flight" class="booking-layout">
        <div class="flight-summary">
          <div class="summary-title">{{ flight.departureCity }} -> {{ flight.arrivalCity }}</div>
          <div class="summary-line">{{ flight.airlineName }} | {{ flight.flightNo }} | {{ flight.cabinClass }}</div>
          <div class="summary-line">{{ formatDateTime(flight.departureTime) }} - {{ formatDateTime(flight.arrivalTime) }}</div>
          <div class="summary-line">{{ flight.departureAirport }} -> {{ flight.arrivalAirport }}</div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="booking-form">
          <el-form-item label="乘机人" prop="contactId">
            <el-select v-model="form.contactId" placeholder="请选择常用联系人">
              <el-option v-for="item in contacts" :key="item.id" :label="`${item.name}（${item.phone}）`" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="可填写出行备注" />
          </el-form-item>

          <div class="price-board">
            <div>票面价：￥{{ formatPrice(originalAmount) }}</div>
            <div class="summary-price">订单金额：￥{{ formatPrice(originalAmount) }}</div>
          </div>

          <div v-if="!contacts.length" class="contact-tip">
            当前没有常用联系人，请先前往个人中心维护。
            <el-button link type="primary" @click="router.push('/profile')">去维护联系人</el-button>
          </div>
          <div class="form-actions">
            <el-button @click="router.back()">返回</el-button>
            <el-button type="primary" :disabled="!contacts.length" :loading="submitting" @click="submitOrder">提交订单</el-button>
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
import { getFlightDetail } from '@/api/flight'
import { createFlightOrder } from '@/api/order'
import { getUserContacts } from '@/api/userContact'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const flight = ref(null)
const contacts = ref([])

const form = reactive({
  contactId: null,
  remark: ''
})

const rules = {
  contactId: [{ required: true, message: '请选择乘机人', trigger: 'change' }]
}

const originalAmount = computed(() => Number(flight.value?.price || 0))

async function loadData() {
  loading.value = true
  try {
    const [flightResponse, contactResponse] = await Promise.all([
      getFlightDetail(route.params.id),
      getUserContacts()
    ])
    flight.value = flightResponse.data
    contacts.value = contactResponse.data
    const defaultContact = contacts.value.find((item) => item.isDefault === 1) || contacts.value[0]
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
    const contact = contacts.value.find((item) => item.id === form.contactId)
    const response = await createFlightOrder({
      productType: 'FLIGHT',
      productId: Number(route.params.id),
      quantity: 1,
      travelDate: flight.value.departureTime?.slice(0, 10),
      contactName: contact?.name,
      contactPhone: contact?.phone
    })
    ElMessage.success('机票订单提交成功')
    router.push({ name: 'orders', query: { highlight: response.data.id } })
  } finally {
    submitting.value = false
  }
}

function formatDateTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--'
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
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

.flight-summary {
  padding: 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #eff6ff, #f8fafc);
}

.summary-title {
  font-size: 26px;
  font-weight: 700;
}

.summary-line {
  margin-top: 8px;
  color: #475569;
}

.summary-price {
  color: #d9480f;
  font-size: 22px;
  font-weight: 700;
}

.booking-form :deep(.el-select) {
  width: 100%;
}

.price-board {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 14px;
  background: #fff7ed;
  color: #7c2d12;
  display: grid;
  gap: 6px;
}

.contact-tip {
  margin-bottom: 16px;
  color: #b45309;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
