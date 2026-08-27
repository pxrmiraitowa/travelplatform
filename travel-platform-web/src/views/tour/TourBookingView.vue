<template>
  <div class="booking-page" v-loading="loading">
    <SectionCard title="旅游产品下单" description="选择出行日期和联系人后提交订单，订单金额以服务端商品快照为准。">
      <div v-if="tour" class="booking-layout">
        <div class="summary-box">
          <div class="summary-title">{{ tour.packageName }}</div>
          <div class="summary-line">{{ tour.destination }} | {{ tour.departureCity || '全国出发' }} | {{ tour.days }}天</div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="booking-form">
          <el-form-item label="出行日期" prop="travelDate">
            <el-radio-group v-model="form.travelDate" class="date-group">
              <el-radio-button v-for="date in tour.travelDateOptions || []" :key="date" :label="date">{{ date }}</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="出行人" prop="contactId">
            <el-select v-model="form.contactId" placeholder="请选择常用联系人">
              <el-option v-for="item in contacts" :key="item.id" :label="`${item.name}（${item.phone}）`" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="优惠券">
            <el-select v-model="form.couponId" clearable placeholder="不使用优惠券">
              <el-option
                v-for="item in availableCoupons"
                :key="item.id"
                :label="couponLabel(item)"
                :value="item.id"
                :disabled="!canUseCoupon(item)"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="可填写出行备注" />
          </el-form-item>

          <div class="price-board">
            <div>产品原价：￥{{ formatPrice(originalAmount) }}</div>
            <div>优惠金额：-￥{{ formatPrice(discountAmount) }}</div>
            <div class="total-line">预计订单金额：￥{{ formatPrice(payableAmount) }}</div>
            <div class="price-tip">最终金额以后端订单结算结果为准。</div>
          </div>

          <div class="form-actions">
            <el-button @click="router.back()">返回</el-button>
            <el-button type="primary" :loading="submitting" :disabled="!contacts.length" @click="submitOrder">提交订单</el-button>
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
import SectionCard from '@/components/SectionCard.vue'
import { createTourOrder } from '@/api/order'
import { getTourPriceCompare } from '@/api/price'
import { getTourDetail } from '@/api/tour'
import { getUserContacts } from '@/api/userContact'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const tour = ref(null)
const contacts = ref([])
const coupons = ref([])

const form = reactive({
  travelDate: '',
  contactId: null,
  couponId: null,
  remark: ''
})

const rules = {
  travelDate: [{ required: true, message: '请选择出行日期', trigger: 'change' }],
  contactId: [{ required: true, message: '请选择出行人', trigger: 'change' }]
}

const originalAmount = computed(() => Number(tour.value?.price || 0))
const availableCoupons = computed(() => coupons.value || [])
const selectedCoupon = computed(() => availableCoupons.value.find((item) => item.id === form.couponId) || null)
const discountAmount = computed(() => {
  const coupon = selectedCoupon.value
  if (!coupon || !canUseCoupon(coupon)) {
    return 0
  }
  return Math.min(Number(coupon.discountAmount || 0), originalAmount.value)
})
const payableAmount = computed(() => Math.max(originalAmount.value - discountAmount.value, 0))

async function loadData() {
  loading.value = true
  try {
    const [tourResponse, contactResponse, compareResponse] = await Promise.all([
      getTourDetail(route.params.id),
      getUserContacts(),
      getTourPriceCompare(route.params.id)
    ])
    tour.value = tourResponse.data
    contacts.value = contactResponse.data
    coupons.value = compareResponse.data?.couponList || []
    const queryDate = route.query.travelDate || ''
    form.travelDate = tour.value.travelDateOptions?.includes(queryDate) ? queryDate : (tour.value.travelDateOptions?.[0] || '')
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
    const response = await createTourOrder({
      productType: 'TOUR',
      productId: Number(route.params.id),
      quantity: 1,
      couponId: form.couponId || null,
      travelDate: form.travelDate,
      contactName: contact?.name,
      contactPhone: contact?.phone
    })
    ElMessage.success('旅游产品订单提交成功')
    router.push({ name: 'orders', query: { highlight: response.data.id } })
  } finally {
    submitting.value = false
  }
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

function canUseCoupon(coupon) {
  return originalAmount.value >= Number(coupon.thresholdAmount || 0)
}

function couponLabel(coupon) {
  const threshold = Number(coupon.thresholdAmount || 0)
  const discount = Number(coupon.discountAmount || 0)
  const suffix = canUseCoupon(coupon) ? '' : '（未达门槛）'
  return `${coupon.couponName}：满￥${formatPrice(threshold)} 减￥${formatPrice(discount)}${suffix}`
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

.summary-box {
  padding: 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f0fdf4, #f8fafc);
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

.date-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.price-board {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 14px;
  background: #ecfdf5;
  color: #166534;
  display: grid;
  gap: 6px;
}

.total-line {
  color: #d9480f;
  font-size: 20px;
  font-weight: 700;
}

.price-tip {
  color: #166534;
  font-size: 13px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
