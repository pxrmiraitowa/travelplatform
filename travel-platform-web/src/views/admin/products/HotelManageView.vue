<template>
  <AdminCrudPage
    title="酒店管理"
    description="维护酒店基本信息、星级和营业状态。"
    :filters="filters"
    :columns="columns"
    :form-fields="formFields"
    :initial-query="initialQuery"
    :initial-form="initialForm"
    :load-api="getAdminHotels"
    :create-api="createAdminHotel"
    :update-api="updateAdminHotel"
    :delete-api="deleteAdminHotel"
  />
</template>

<script setup>
import AdminCrudPage from '@/components/admin/AdminCrudPage.vue'
import { createAdminHotel, deleteAdminHotel, getAdminHotels, updateAdminHotel } from '@/api/admin'

const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }]
const statusTagMap = { 1: { label: '启用', type: 'success' }, 0: { label: '停用', type: 'info' } }

const filters = [
  { label: '关键词', prop: 'keyword', type: 'input', placeholder: '酒店名 / 城市 / 地址' },
  { label: '状态', prop: 'status', type: 'select', options: statusOptions }
]

const columns = [
  { label: '酒店名称', prop: 'hotelName', minWidth: 180 },
  { label: '城市', prop: 'city', width: 120 },
  { label: '区域', prop: 'district', width: 120 },
  { label: '星级', prop: 'starLevel', width: 90 },
  { label: '地址', prop: 'address', minWidth: 220 },
  { label: '状态', prop: 'status', width: 100, tagMap: statusTagMap }
]

const formFields = [
  { label: '酒店名称', prop: 'hotelName', required: true },
  { label: '城市', prop: 'city', required: true },
  { label: '区域', prop: 'district' },
  { label: '地址', prop: 'address', required: true },
  { label: '描述', prop: 'description', type: 'textarea' },
  { label: '星级', prop: 'starLevel', type: 'number', min: 1, required: true },
  { label: '封面图', prop: 'coverImage' },
  { label: '入住时间', prop: 'checkInTime' },
  { label: '离店时间', prop: 'checkOutTime' },
  { label: '状态', prop: 'status', type: 'select', options: statusOptions, required: true }
]

const initialQuery = { keyword: '', status: undefined }
const initialForm = {
  hotelName: '',
  city: '',
  district: '',
  address: '',
  description: '',
  starLevel: 4,
  coverImage: '',
  checkInTime: '14:00',
  checkOutTime: '12:00',
  status: 1
}
</script>
