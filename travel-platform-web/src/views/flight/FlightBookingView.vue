<template>
  <div class="booking-page" v-loading="loading">
    <SectionCard title="机票下单" description="选择联系人和可用优惠券后提交订单，后端会再次校验优惠券门槛。">
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

          <el-form-item label="优惠券">
            <el-select v-model="form.couponId" clearable placeholder="不使用优惠券">
              <el-option v-for="item in eligibleCoupons" :key="item.id" :label="couponLabel(item)" :value="item.id" />
            </el-select>
            <div class="coupon-tip">
              <span v-if="eligibleCoupons.length">已自动选择当前最优优惠券。</span>
              <span v-else-if="coupons.length">当前订单金额未达到优惠券门槛。</span>
              <span v-else>当前没有可用规则优惠券。</span>
            </div>
          </el-form-item>

          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="可填写出行备注" />
          </el-form-item>

          <div class="price-board">
            <div>票面价：￥{{ formatPrice(originalAmount) }}</div>
            <div>优惠金额：-￥{{ formatPrice(discountAmount) }}</div>
            <div v-if="selectedCoupon" class="coupon-name">已使用：{{ selectedCoupon.couponName }}</div>
            <div class="summary-price">应付金额：￥{{ formatPrice(payableAmount) }}</div>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import SectionCard from '@/components/SectionCard.vue'
import { getFlightDetail } from '@/api/flight'
import { createFlightOrder } from '@/api/order'
import { getFlightPriceCompare } from '@/api/price'
import { getUserContacts } from '@/api/userContact'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const flight = ref(null)
const contacts = ref([])
const coupons = ref([])

const form = reactive({
  contactId: null,
  couponId: null,
  remark: ''
})

const rules = {
  contactId: [{ required: true, message: '请选择乘机人', trigger: 'change' }]
}

const originalAmount = computed(() => Number(flight.value?.price || 0))
const eligibleCoupons = computed(() =>
  coupons.value
    .filter((item) => originalAmount.value >= Number(item.thresholdAmount || 0))
    .sort((a, b) => Number(b.discountAmount || 0) - Number(a.discountAmount || 0))
)
const selectedCoupon = computed(() => eligibleCoupons.value.find((item) => item.id === form.couponId) || null)
const discountAmount = computed(() => Number(selectedCoupon.value?.discountAmount || 0))
const payableAmount = computed(() => Math.max(originalAmount.value - discountAmount.value, 0))

watch(eligibleCoupons, (list) => {
  if (!list.length) {
    form.couponId = null
    return
  }
  if (!form.couponId || !list.some((item) => item.id === form.couponId)) {
    form.couponId = list[0].id
  }
}, { immediate: true })

async function loadData() {
  loading.value = true
  try {
    const [flightResponse, contactResponse, compareResponse] = await Promise.all([
      getFlightDetail(route.params.id),
      getUserContacts(),
      getFlightPriceCompare(route.params.id)
    ])
    flight.value = flightResponse.data
    contacts.value = contactResponse.data
    coupons.value = compareResponse.data.couponList || []
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

function couponLabel(item) {
  return `${item.couponName}（满￥${formatPrice(item.thresholdAmount)}减￥${formatPrice(item.discountAmount)}）`
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

.coupon-tip {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
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

.coupon-name {
  color: #c2410c;
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
