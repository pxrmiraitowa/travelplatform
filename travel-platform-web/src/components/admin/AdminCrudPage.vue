<template>
  <div class="admin-page">
    <section class="admin-card">
      <div class="admin-section__head">
        <div>
          <h2>{{ title }}</h2>
          <p>{{ description }}</p>
        </div>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>

      <div class="admin-toolbar">
        <template v-for="filter in filters" :key="filter.prop">
          <el-input
            v-if="filter.type === 'input'"
            v-model="queryForm[filter.prop]"
            :placeholder="filter.placeholder || `请输入${filter.label}`"
            clearable
            class="admin-filter"
          />
          <el-select
            v-else-if="filter.type === 'select'"
            v-model="queryForm[filter.prop]"
            :placeholder="filter.placeholder || `请选择${filter.label}`"
            clearable
            class="admin-filter"
          >
            <el-option
              v-for="option in filter.options || []"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </template>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table :data="tableData" border v-loading="loading">
        <el-table-column
          v-for="column in columns"
          :key="column.prop || column.label"
          :label="column.label"
          :prop="column.prop"
          :min-width="column.minWidth || 140"
          :width="column.width"
        >
          <template #default="{ row }">
            <el-tag
              v-if="column.tagMap"
              :type="column.tagMap[row[column.prop]]?.type || 'info'"
            >
              {{ column.tagMap[row[column.prop]]?.label || row[column.prop] }}
            </el-tag>
            <span v-else>{{ column.formatter ? column.formatter(row) : row[column.prop] }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="queryForm.pageNum"
          :page-size="queryForm.pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
        <el-form-item
          v-for="field in formFields"
          :key="field.prop"
          :label="field.label"
          :prop="field.prop"
        >
          <el-input
            v-if="!field.type || field.type === 'input'"
            v-model="formData[field.prop]"
            :type="field.inputType || 'text'"
            :placeholder="field.placeholder || `请输入${field.label}`"
          />
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="formData[field.prop]"
            type="textarea"
            :rows="4"
            :placeholder="field.placeholder || `请输入${field.label}`"
          />
          <el-input-number
            v-else-if="field.type === 'number'"
            v-model="formData[field.prop]"
            :min="field.min ?? 0"
            :step="field.step ?? 1"
            controls-position="right"
            class="admin-number"
          />
          <el-select
            v-else-if="field.type === 'select'"
            v-model="formData[field.prop]"
            :placeholder="field.placeholder || `请选择${field.label}`"
            class="admin-number"
          >
            <el-option
              v-for="option in field.options || []"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-date-picker
            v-else-if="field.type === 'datetime'"
            v-model="formData[field.prop]"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            class="admin-number"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  filters: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  formFields: { type: Array, default: () => [] },
  initialQuery: { type: Object, default: () => ({}) },
  initialForm: { type: Object, default: () => ({}) },
  loadApi: { type: Function, required: true },
  createApi: { type: Function, required: true },
  updateApi: { type: Function, required: true },
  deleteApi: { type: Function, required: true }
})

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const tableData = ref([])
const total = ref(0)
const formRef = ref()

const queryForm = reactive({
  pageNum: 1,
  pageSize: 10,
  ...clone(props.initialQuery)
})

const formData = reactive(clone(props.initialForm))

const rules = computed(() => props.formFields.reduce((acc, field) => {
  if (field.required) {
    acc[field.prop] = [{ required: true, message: `请填写${field.label}`, trigger: field.type === 'select' ? 'change' : 'blur' }]
  }
  return acc
}, {}))

const dialogTitle = computed(() => editingId.value ? `编辑${props.title}` : `新增${props.title}`)

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

function resetFormData(data = props.initialForm) {
  Object.keys(formData).forEach((key) => delete formData[key])
  Object.assign(formData, clone(data))
}

async function loadData() {
  loading.value = true
  try {
    const response = await props.loadApi({ ...queryForm })
    tableData.value = response.data.records || []
    total.value = response.data.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(queryForm, { pageNum: 1, pageSize: 10, ...clone(props.initialQuery) })
  loadData()
}

function openCreate() {
  editingId.value = null
  resetFormData()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  resetFormData(row)
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除“${row.name || row.title || row.flightNo || row.trainNo || row.hotelName || row.roomName || row.packageName || row.id}”吗？`, '删除提示', {
    type: 'warning'
  })
  await props.deleteApi(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitLoading.value = true
  try {
    const payload = clone(formData)
    if (editingId.value) {
      await props.updateApi(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await props.createApi(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

function handlePageChange(page) {
  queryForm.pageNum = page
  loadData()
}

defineExpose({ loadData })

loadData()
</script>
