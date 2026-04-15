<template>
  <AdminCrudPage
    title="车次管理"
    description="维护车次信息、席位价格和各席别库存。"
    :filters="filters"
    :columns="columns"
    :form-fields="formFields"
    :initial-query="initialQuery"
    :initial-form="initialForm"
    :load-api="getAdminTrains"
    :create-api="createAdminTrain"
    :update-api="updateAdminTrain"
    :delete-api="deleteAdminTrain"
  />
</template>

<script setup>
import AdminCrudPage from '@/components/admin/AdminCrudPage.vue'
import { createAdminTrain, deleteAdminTrain, getAdminTrains, updateAdminTrain } from '@/api/admin'

const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }]
const statusTagMap = { 1: { label: '启用', type: 'success' }, 0: { label: '停用', type: 'info' } }

const filters = [
  { label: '关键词', prop: 'keyword', type: 'input', placeholder: '车次 / 城市 / 车站' },
  { label: '状态', prop: 'status', type: 'select', options: statusOptions }
]

const columns = [
  { label: '车次', prop: 'trainNo', width: 110 },
  { label: '类型', prop: 'trainType', width: 110 },
  { label: '线路', prop: 'route', minWidth: 180, formatter: (row) => `${row.departureCity} -> ${row.arrivalCity}` },
  { label: '出发时间', prop: 'departureTime', minWidth: 180 },
  { label: '二等座', prop: 'secondClassPrice', width: 110 },
  { label: '二等库存', prop: 'secondClassStock', width: 100 },
  { label: '状态', prop: 'status', width: 100, tagMap: statusTagMap }
]

const formFields = [
  { label: '车次', prop: 'trainNo', required: true },
  { label: '类型', prop: 'trainType', required: true },
  { label: '出发城市', prop: 'departureCity', required: true },
  { label: '到达城市', prop: 'arrivalCity', required: true },
  { label: '出发站', prop: 'departureStation', required: true },
  { label: '到达站', prop: 'arrivalStation', required: true },
  { label: '出发时间', prop: 'departureTime', type: 'datetime', required: true },
  { label: '到达时间', prop: 'arrivalTime', type: 'datetime', required: true },
  { label: '时长(分钟)', prop: 'durationMinutes', type: 'number', min: 1, required: true },
  { label: '商务座价', prop: 'businessPrice', type: 'number', min: 0, step: 10 },
  { label: '一等座价', prop: 'firstClassPrice', type: 'number', min: 0, step: 10 },
  { label: '二等座价', prop: 'secondClassPrice', type: 'number', min: 0, step: 10 },
  { label: '商务库存', prop: 'businessStock', type: 'number', min: 0, required: true },
  { label: '一等库存', prop: 'firstClassStock', type: 'number', min: 0, required: true },
  { label: '二等库存', prop: 'secondClassStock', type: 'number', min: 0, required: true },
  { label: '状态', prop: 'status', type: 'select', options: statusOptions, required: true }
]

const initialQuery = { keyword: '', status: undefined }
const initialForm = {
  trainNo: '',
  trainType: '高铁',
  departureCity: '',
  arrivalCity: '',
  departureStation: '',
  arrivalStation: '',
  departureTime: '',
  arrivalTime: '',
  durationMinutes: 60,
  businessPrice: 0,
  firstClassPrice: 0,
  secondClassPrice: 0,
  businessStock: 0,
  firstClassStock: 0,
  secondClassStock: 0,
  status: 1
}
</script>
