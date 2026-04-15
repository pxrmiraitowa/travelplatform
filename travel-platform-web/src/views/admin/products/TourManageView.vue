<template>
  <AdminCrudPage
    title="旅游产品管理"
    description="维护旅游线路、发团日期、库存和价格。"
    :filters="filters"
    :columns="columns"
    :form-fields="formFields"
    :initial-query="initialQuery"
    :initial-form="initialForm"
    :load-api="getAdminTours"
    :create-api="createAdminTour"
    :update-api="updateAdminTour"
    :delete-api="deleteAdminTour"
  />
</template>

<script setup>
import AdminCrudPage from '@/components/admin/AdminCrudPage.vue'
import { createAdminTour, deleteAdminTour, getAdminTours, updateAdminTour } from '@/api/admin'

const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }]
const statusTagMap = { 1: { label: '启用', type: 'success' }, 0: { label: '停用', type: 'info' } }

const filters = [
  { label: '关键词', prop: 'keyword', type: 'input', placeholder: '产品名 / 目的地 / 出发城市' },
  { label: '状态', prop: 'status', type: 'select', options: statusOptions }
]

const columns = [
  { label: '产品名称', prop: 'packageName', minWidth: 220 },
  { label: '目的地', prop: 'destination', width: 160 },
  { label: '出发城市', prop: 'departureCity', width: 120 },
  { label: '天数', prop: 'days', width: 90 },
  { label: '价格', prop: 'price', width: 100 },
  { label: '库存', prop: 'stock', width: 90 },
  { label: '状态', prop: 'status', width: 100, tagMap: statusTagMap }
]

const formFields = [
  { label: '产品名称', prop: 'packageName', required: true },
  { label: '目的地', prop: 'destination', required: true },
  { label: '出发城市', prop: 'departureCity' },
  { label: '天数', prop: 'days', type: 'number', min: 1, required: true },
  { label: '价格', prop: 'price', type: 'number', min: 0, step: 10, required: true },
  { label: '库存', prop: 'stock', type: 'number', min: 0, required: true },
  { label: '出行日期', prop: 'travelDates', placeholder: '多个日期用逗号分隔' },
  { label: '产品描述', prop: 'description', type: 'textarea' },
  { label: '封面图', prop: 'coverImage' },
  { label: '状态', prop: 'status', type: 'select', options: statusOptions, required: true }
]

const initialQuery = { keyword: '', status: undefined }
const initialForm = {
  packageName: '',
  destination: '',
  departureCity: '',
  days: 3,
  price: 0,
  stock: 0,
  travelDates: '',
  description: '',
  coverImage: '',
  status: 1
}
</script>
