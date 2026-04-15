<template>
  <div v-if="order" class="order-detail">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="订单状态">{{ statusText(order.orderStatus) }}</el-descriptions-item>
      <el-descriptions-item label="订单类型">{{ bizTypeText(order.bizType) }}</el-descriptions-item>
      <el-descriptions-item label="出行日期">{{ order.travelDate || '--' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatDateTime(order.createTime) }}</el-descriptions-item>
      <el-descriptions-item label="联系人">{{ order.contactName }} / {{ order.contactPhone }}</el-descriptions-item>
      <el-descriptions-item label="原价">￥{{ formatAmount(order.originalAmount ?? order.totalAmount) }}</el-descriptions-item>
      <el-descriptions-item label="优惠金额">￥{{ formatAmount(order.discountAmount) }}</el-descriptions-item>
      <el-descriptions-item label="优惠券">{{ order.couponName || '未使用' }}</el-descriptions-item>
      <el-descriptions-item label="实付金额">￥{{ formatAmount(order.totalAmount) }}</el-descriptions-item>
      <el-descriptions-item label="备注">{{ order.remark || '无' }}</el-descriptions-item>
    </el-descriptions>

    <div v-if="order.flightInfo" class="detail-box">
      <h4>机票信息</h4>
      <p>{{ order.flightInfo.departureCity }} -> {{ order.flightInfo.arrivalCity }}</p>
      <p>{{ order.flightInfo.airlineName }} | {{ order.flightInfo.flightNo }}</p>
      <p>{{ formatDateTime(order.flightInfo.departureTime) }} - {{ formatDateTime(order.flightInfo.arrivalTime) }}</p>
      <p>{{ order.flightInfo.departureAirport }} -> {{ order.flightInfo.arrivalAirport }}</p>
      <p>{{ order.flightInfo.passengerName }} / {{ order.flightInfo.passengerPhone }}</p>
    </div>

    <div v-if="order.trainInfo" class="detail-box">
      <h4>火车票信息</h4>
      <p>{{ order.trainInfo.departureCity }} -> {{ order.trainInfo.arrivalCity }}</p>
      <p>{{ order.trainInfo.trainType }} | {{ order.trainInfo.trainNo }}</p>
      <p>{{ formatDateTime(order.trainInfo.departureTime) }} - {{ formatDateTime(order.trainInfo.arrivalTime) }}</p>
      <p>{{ order.trainInfo.departureStation }} -> {{ order.trainInfo.arrivalStation }}</p>
      <p>{{ order.trainInfo.seatType }} / ￥{{ formatAmount(order.trainInfo.seatPrice) }}</p>
      <p>{{ order.trainInfo.passengerName }} / {{ order.trainInfo.passengerPhone }}</p>
    </div>

    <div v-if="order.hotelInfo" class="detail-box">
      <h4>酒店信息</h4>
      <p>{{ order.hotelInfo.hotelName }}</p>
      <p>{{ order.hotelInfo.city }} | {{ order.hotelInfo.address }}</p>
      <p>{{ order.hotelInfo.roomName }} | {{ order.hotelInfo.bedType }} | {{ order.hotelInfo.breakfast }}</p>
      <p>{{ order.hotelInfo.checkInDate }} 入住 - {{ order.hotelInfo.checkOutDate }} 离店</p>
      <p>{{ order.hotelInfo.guestName }} / {{ order.hotelInfo.guestPhone }}</p>
      <p>单晚 ￥{{ formatAmount(order.hotelInfo.roomPrice) }}，共 {{ order.hotelInfo.nightCount }} 晚</p>
    </div>

    <div v-if="order.tourInfo" class="detail-box">
      <h4>旅游产品信息</h4>
      <p>{{ order.tourInfo.packageName }}</p>
      <p>{{ order.tourInfo.destination }} | {{ order.tourInfo.departureCity || '全国出发' }}</p>
      <p>{{ order.tourInfo.travelDate }} 出发 | {{ order.tourInfo.days }} 天</p>
      <p>{{ order.tourInfo.guestName }} / {{ order.tourInfo.guestPhone }}</p>
      <p>产品价格 ￥{{ formatAmount(order.tourInfo.packagePrice) }}</p>
    </div>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'

defineProps({
  order: {
    type: Object,
    default: null
  }
})

function bizTypeText(bizType) {
  return {
    FLIGHT: '机票',
    TRAIN: '火车票',
    HOTEL: '酒店',
    TOUR: '旅游'
  }[bizType] || bizType
}

function statusText(status) {
  return {
    10: '待支付',
    20: '待出行',
    30: '已完成',
    40: '已取消'
  }[status] || '未知状态'
}

function formatDateTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--'
}

function formatAmount(value) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped>
.order-detail {
  display: grid;
  gap: 20px;
}

.detail-box {
  padding: 16px;
  border-radius: 12px;
  background: #f8fafc;
  color: #1f2937;
}

.detail-box h4 {
  margin: 0 0 10px;
}

.detail-box p {
  margin: 6px 0;
}
</style>
