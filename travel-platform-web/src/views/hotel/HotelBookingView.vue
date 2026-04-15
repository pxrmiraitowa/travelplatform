<template>
  <div class="booking-page" v-loading="loading">
    <SectionCard title="酒店预订" description="选择入住日期、联系人和可用优惠券后提交订单。">
      <div v-if="hotel && selectedRoom" class="booking-layout">
        <div class="summary-box">
          <div class="summary-title">{{ hotel.hotelName }}</div>
          <div class="summary-line">{{ hotel.city }} {{ hotel.district || '' }} | {{ hotel.address }}</div>
          <div class="summary-line">{{ selectedRoom.roomName }} | {{ selectedRoom.bedType }} | {{ selectedRoom.breakfast }}</div>
          <div class="summary-line">单晚房价：￥{{ formatPrice(selectedRoom.price) }}</div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="booking-form">
          <el-form-item label="房型" prop="hotelRoomId">
            <el-select v-model="form.hotelRoomId" placeholder="请选择房型">
              <el-option
                v-for="item in availableRooms"
                :key="item.id"
                :label="`${item.roomName}（￥${formatPrice(item.price)} / 晚）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="入住日期" prop="checkInDate">
            <el-date-picker v-model="form.checkInDate" type="date" value-format="YYYY-MM-DD" placeholder="选择入住日期" />
          </el-form-item>

          <el-form-item label="离店日期" prop="checkOutDate">
            <el-date-picker v-model="form.checkOutDate" type="date" value-format="YYYY-MM-DD" placeholder="选择离店日期" />
          </el-form-item>

          <el-form-item label="入住人" prop="contactId">
            <el-select v-model="form.contactId" placeholder="请选择常用联系人">
              <el-option
                v-for="item in contacts"
                :key="item.id"
                :label="`${item.name}（${item.phone}）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="优惠券">
            <el-select v-model="form.couponId" clearable placeholder="不使用优惠券">
              <el-option
                v-for="item in eligibleCoupons"
                :key="item.id"
                :label="couponLabel(item)"
                :value="item.id"
              />
            </el-select>
            <div class="coupon-tip">
              <span v-if="eligibleCoupons.length">已为当前订单自动推荐最优优惠券，也可手动切换。</span>
              <span v-else-if="coupons.length">当前订单金额未达到优惠券门槛。</span>
              <span v-else>当前没有可用规则优惠券。</span>
            </div>
          </el-form-item>

          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="可填写入住备注" />
          </el-form-item>

          <div class="price-board">
            <div>{{ nightCount }} 晚原价：￥{{ formatPrice(originalAmount) }}</div>
            <div>优惠金额：-￥{{ formatPrice(discountAmount) }}</div>
            <div v-if="selectedCoupon" class="coupon-name">已使用：{{ selectedCoupon.couponName }}</div>
            <div class="summary-total">应付金额：￥{{ formatPrice(payableAmount) }}</div>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import SectionCard from '@/components/SectionCard.vue'
import { getHotelDetail } from '@/api/hotel'
import { createHotelOrder } from '@/api/order'
import { getHotelPriceCompare } from '@/api/price'
import { getUserContacts } from '@/api/userContact'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const hotel = ref(null)
const contacts = ref([])
const coupons = ref([])

const form = reactive({
  hotelRoomId: null,
  checkInDate: '',
  checkOutDate: '',
  contactId: null,
  couponId: null,
  remark: ''
})

const rules = {
  hotelRoomId: [{ required: true, message: '请选择房型', trigger: 'change' }],
  checkInDate: [{ required: true, message: '请选择入住日期', trigger: 'change' }],
  checkOutDate: [{ required: true, message: '请选择离店日期', trigger: 'change' }],
  contactId: [{ required: true, message: '请选择入住人', trigger: 'change' }]
}

const availableRooms = computed(() => (hotel.value?.roomList || []).filter((item) => item.stock > 0))
const selectedRoom = computed(() => availableRooms.value.find((item) => item.id === form.hotelRoomId) || null)
const nightCount = computed(() => {
  if (!form.checkInDate || !form.checkOutDate) {
    return 0
  }
  const diff = dayjs(form.checkOutDate).diff(dayjs(form.checkInDate), 'day')
  return diff > 0 ? diff : 0
})
const originalAmount = computed(() => Number(selectedRoom.value?.price || 0) * nightCount.value)
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
    const [hotelResponse, contactResponse, compareResponse] = await Promise.all([
      getHotelDetail(route.params.id),
      getUserContacts(),
      getHotelPriceCompare(route.params.id)
    ])
    hotel.value = hotelResponse.data
    contacts.value = contactResponse.data
    coupons.value = compareResponse.data.couponList || []
    const roomId = Number(route.query.roomId || 0)
    const defaultRoom = availableRooms.value.find((item) => item.id === roomId) || availableRooms.value[0]
    const defaultContact = contacts.value.find((item) => item.isDefault === 1) || contacts.value[0]
    form.hotelRoomId = defaultRoom?.id ?? null
    form.checkInDate = route.query.checkInDate || dayjs().add(1, 'day').format('YYYY-MM-DD')
    form.checkOutDate = route.query.checkOutDate || dayjs().add(2, 'day').format('YYYY-MM-DD')
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
    const response = await createHotelOrder({
      hotelId: Number(route.params.id),
      hotelRoomId: form.hotelRoomId,
      checkInDate: form.checkInDate,
      checkOutDate: form.checkOutDate,
      contactId: form.contactId,
      couponId: form.couponId,
      remark: form.remark
    })
    ElMessage.success('酒店订单提交成功')
    router.push({ name: 'orders', query: { highlight: response.data.id } })
  } finally {
    submitting.value = false
  }
}

function couponLabel(item) {
  return `${item.couponName}（满￥${formatPrice(item.thresholdAmount)}减￥${formatPrice(item.discountAmount)}）`
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

.summary-box {
  padding: 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #fff7ed, #f8fafc);
}

.summary-title {
  font-size: 26px;
  font-weight: 700;
}

.summary-line {
  margin-top: 8px;
  color: #475569;
}

.booking-form :deep(.el-select),
.booking-form :deep(.el-date-editor) {
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

.summary-total {
  color: #d9480f;
  font-size: 20px;
  font-weight: 700;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
