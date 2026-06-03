<template>
  <AdminCrudPage
    title="房型管理"
    description="维护酒店房型库存、价格和入住规则。"
    :filters="filters"
    :columns="columns"
    :form-fields="formFields"
    :initial-query="initialQuery"
    :initial-form="initialForm"
    :load-api="getAdminHotelRooms"
    :create-api="createAdminHotelRoom"
    :update-api="updateAdminHotelRoom"
    :delete-api="deleteAdminHotelRoom"
  />
</template>

<script setup>
import { useRoute } from 'vue-router'
import AdminCrudPage from '@/components/admin/AdminCrudPage.vue'
import { createAdminHotelRoom, deleteAdminHotelRoom, getAdminHotelRooms, updateAdminHotelRoom } from '@/api/admin'

const route = useRoute()
const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }]
const statusTagMap = { 1: { label: '启用', type: 'success' }, 0: { label: '停用', type: 'info' } }

const filters = [
  { label: '酒店ID', prop: 'hotelId', type: 'input', placeholder: '按酒店 ID 筛选' },
  { label: '关键词', prop: 'keyword', type: 'input', placeholder: '房型名 / 床型 / 酒店名' },
  { label: '状态', prop: 'status', type: 'select', options: statusOptions }
]

const columns = [
  { label: '酒店ID', prop: 'hotelId', width: 90 },
  { label: '房型名称', prop: 'roomName', minWidth: 160 },
  { label: '酒店', prop: 'hotelName', minWidth: 180 },
  { label: '床型', prop: 'bedType', width: 120 },
  { label: '价格', prop: 'price', width: 100 },
  { label: '库存', prop: 'stock', width: 90 },
  { label: '可住人数', prop: 'guestCount', width: 100 },
  { label: '状态', prop: 'status', width: 100, tagMap: statusTagMap }
]

const formFields = [
  { label: '酒店ID', prop: 'hotelId', type: 'number', min: 1, required: true },
  { label: '房型名称', prop: 'roomName', required: true },
  { label: '床型', prop: 'bedType', required: true },
  { label: '早餐', prop: 'breakfast' },
  { label: '面积', prop: 'roomArea' },
  { label: '可住人数', prop: 'guestCount', type: 'number', min: 1, required: true },
  { label: '价格', prop: 'price', type: 'number', min: 0, step: 10, required: true },
  { label: '库存', prop: 'stock', type: 'number', min: 0, required: true },
  { label: '取消规则', prop: 'cancelRule', type: 'textarea' },
  { label: '状态', prop: 'status', type: 'select', options: statusOptions, required: true }
]

const initialHotelId = Number(route.query.hotelId)
const initialQuery = {
  hotelId: Number.isFinite(initialHotelId) && initialHotelId > 0 ? initialHotelId : '',
  keyword: '',
  status: undefined
}

const initialForm = {
  hotelId: Number.isFinite(initialHotelId) && initialHotelId > 0 ? initialHotelId : undefined,
  roomName: '',
  bedType: '',
  breakfast: '',
  roomArea: '',
  guestCount: 2,
  price: 0,
  stock: 0,
  cancelRule: '',
  status: 1
}
</script>
