<template>
  <AdminCrudPage
    title="航班管理"
    description="维护航班班次、价格、库存和上下架状态。"
    :filters="filters"
    :columns="columns"
    :form-fields="formFields"
    :initial-query="initialQuery"
    :initial-form="initialForm"
    :load-api="getAdminFlights"
    :create-api="createAdminFlight"
    :update-api="updateAdminFlight"
    :delete-api="deleteAdminFlight"
  />
</template>

<script setup>
import AdminCrudPage from '@/components/admin/AdminCrudPage.vue'
import { createAdminFlight, deleteAdminFlight, getAdminFlights, updateAdminFlight } from '@/api/admin'

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const statusTagMap = {
  1: { label: '启用', type: 'success' },
  0: { label: '停用', type: 'info' }
}

const filters = [
  { label: '关键词', prop: 'keyword', type: 'input', placeholder: '航班号 / 城市 / 航司' },
  { label: '状态', prop: 'status', type: 'select', options: statusOptions }
]

const columns = [
  { label: '航班号', prop: 'flightNo', width: 120 },
  { label: '航司', prop: 'airlineName', minWidth: 140 },
  { label: '航线', prop: 'route', minWidth: 180, formatter: (row) => `${row.departureCity} -> ${row.arrivalCity}` },
  { label: '起飞时间', prop: 'departureTime', minWidth: 180 },
  { label: '价格', prop: 'price', width: 110 },
  { label: '库存', prop: 'stock', width: 90 },
  { label: '状态', prop: 'status', width: 100, tagMap: statusTagMap }
]

const formFields = [
  { label: '航班号', prop: 'flightNo', required: true },
  { label: '航司', prop: 'airlineName', required: true },
  { label: '出发城市', prop: 'departureCity', required: true },
  { label: '到达城市', prop: 'arrivalCity', required: true },
  { label: '出发机场', prop: 'departureAirport', required: true },
  { label: '到达机场', prop: 'arrivalAirport', required: true },
  { label: '起飞时间', prop: 'departureTime', type: 'datetime', required: true },
  { label: '到达时间', prop: 'arrivalTime', type: 'datetime', required: true },
  { label: '价格', prop: 'price', type: 'number', min: 0, step: 10, required: true },
  { label: '库存', prop: 'stock', type: 'number', min: 0, required: true },
  { label: '舱位', prop: 'cabinClass', required: true },
  { label: '行李规则', prop: 'baggagePolicy' },
  { label: '退改规则', prop: 'refundPolicy', type: 'textarea' },
  { label: '状态', prop: 'status', type: 'select', options: statusOptions, required: true }
]

const initialQuery = { keyword: '', status: undefined }
const initialForm = {
  flightNo: '',
  airlineName: '',
  departureCity: '',
  arrivalCity: '',
  departureAirport: '',
  arrivalAirport: '',
  departureTime: '',
  arrivalTime: '',
  price: 0,
  stock: 0,
  cabinClass: '经济舱',
  baggagePolicy: '',
  refundPolicy: '',
  status: 1
}
</script>
