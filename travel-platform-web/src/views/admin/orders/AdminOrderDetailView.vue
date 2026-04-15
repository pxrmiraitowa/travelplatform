<template>
  <div class="admin-page admin-stack" v-loading="loading">
    <section class="admin-card" v-if="detail">
      <div class="admin-section__head">
        <div>
          <h2>订单详情</h2>
          <p>订单号：{{ detail.orderNo }}</p>
        </div>
        <el-button @click="$router.push('/admin/orders')">返回列表</el-button>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="业务类型">{{ detail.bizType }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ statusLabel(detail.orderStatus) }}</el-descriptions-item>
        <el-descriptions-item label="下单用户">{{ detail.nickname || detail.username }}</el-descriptions-item>
        <el-descriptions-item label="联系手机">{{ detail.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="出行日期">{{ detail.travelDate }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="admin-card" v-if="detail?.flightInfo">
      <div class="admin-section__head"><div><h2>航班信息</h2></div></div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="航班号">{{ detail.flightInfo.flightNo }}</el-descriptions-item>
        <el-descriptions-item label="航司">{{ detail.flightInfo.airlineName }}</el-descriptions-item>
        <el-descriptions-item label="出发">{{ detail.flightInfo.departureCity }} / {{ detail.flightInfo.departureAirport }}</el-descriptions-item>
        <el-descriptions-item label="到达">{{ detail.flightInfo.arrivalCity }} / {{ detail.flightInfo.arrivalAirport }}</el-descriptions-item>
        <el-descriptions-item label="起飞时间">{{ detail.flightInfo.departureTime }}</el-descriptions-item>
        <el-descriptions-item label="到达时间">{{ detail.flightInfo.arrivalTime }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="admin-card" v-if="detail?.trainInfo">
      <div class="admin-section__head"><div><h2>车次信息</h2></div></div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="车次">{{ detail.trainInfo.trainNo }}</el-descriptions-item>
        <el-descriptions-item label="席位">{{ detail.trainInfo.seatType }}</el-descriptions-item>
        <el-descriptions-item label="出发">{{ detail.trainInfo.departureCity }} / {{ detail.trainInfo.departureStation }}</el-descriptions-item>
        <el-descriptions-item label="到达">{{ detail.trainInfo.arrivalCity }} / {{ detail.trainInfo.arrivalStation }}</el-descriptions-item>
        <el-descriptions-item label="出发时间">{{ detail.trainInfo.departureTime }}</el-descriptions-item>
        <el-descriptions-item label="到达时间">{{ detail.trainInfo.arrivalTime }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="admin-card" v-if="detail?.hotelInfo">
      <div class="admin-section__head"><div><h2>酒店信息</h2></div></div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="酒店">{{ detail.hotelInfo.hotelName }}</el-descriptions-item>
        <el-descriptions-item label="房型">{{ detail.hotelInfo.roomName }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.hotelInfo.address }}</el-descriptions-item>
        <el-descriptions-item label="入住日期">{{ detail.hotelInfo.checkInDate }}</el-descriptions-item>
        <el-descriptions-item label="离店日期">{{ detail.hotelInfo.checkOutDate }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="admin-card" v-if="detail?.tourInfo">
      <div class="admin-section__head"><div><h2>旅游产品信息</h2></div></div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="产品">{{ detail.tourInfo.packageName }}</el-descriptions-item>
        <el-descriptions-item label="目的地">{{ detail.tourInfo.destination }}</el-descriptions-item>
        <el-descriptions-item label="出发城市">{{ detail.tourInfo.departureCity }}</el-descriptions-item>
        <el-descriptions-item label="出行日期">{{ detail.tourInfo.travelDate }}</el-descriptions-item>
      </el-descriptions>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getAdminOrderDetail } from '@/api/admin'

const route = useRoute()
const loading = ref(false)
const detail = ref(null)

function statusLabel(status) {
  return {
    10: '待支付',
    20: '待出行',
    30: '已完成',
    40: '已取消'
  }[status] || status
}

onMounted(async () => {
  loading.value = true
  try {
    const response = await getAdminOrderDetail(route.params.id)
    detail.value = response.data
  } finally {
    loading.value = false
  }
})
</script>
