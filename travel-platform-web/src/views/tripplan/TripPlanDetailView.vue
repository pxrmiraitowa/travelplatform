<template>
  <div class="trip-plan-detail-page" v-loading="loading">
    <SectionCard title="计划信息" description="先维护这次旅行的基础信息，再逐天补充安排。">
      <template #extra>
        <div class="header-actions">
          <el-button @click="goBack">返回列表</el-button>
          <el-button type="primary" :loading="savingPlan" @click="handleSavePlan">保存计划</el-button>
        </div>
      </template>

      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="100px" class="plan-form">
        <el-row :gutter="20">
          <el-col :md="12" :xs="24">
            <el-form-item label="计划名称" prop="planName">
              <el-input v-model="planForm.planName" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="总天数" prop="totalDays">
              <el-input-number v-model="planForm.totalDays" :min="1" :max="60" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="开始日期">
              <el-date-picker
                v-model="planForm.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="可选"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="数据来源">
              <el-input :model-value="planForm.sourceType === 'MANUAL' ? '手动规划' : (planForm.sourceType || '--')" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="整体备注">
              <el-input
                v-model="planForm.remark"
                type="textarea"
                :rows="3"
                maxlength="255"
                show-word-limit
                placeholder="例如：第一晚休息为主，第二天开始核心景点"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </SectionCard>

    <SectionCard title="每日安排" description="支持新增、编辑、删除某一天的行程内容。">
      <template #extra>
        <el-button type="primary" @click="openCreateItemDialog">新增每日安排</el-button>
      </template>

      <el-empty v-if="!planItems.length" description="还没有每日安排，先新增第一天吧。" />

      <div v-else class="table-wrap">
        <el-table :data="planItems" border stripe>
          <el-table-column prop="dayNo" label="天数" width="90">
            <template #default="{ row }">第 {{ row.dayNo }} 天</template>
          </el-table-column>
          <el-table-column prop="destination" label="目的地" min-width="140" />
          <el-table-column prop="hotel" label="酒店" min-width="160">
            <template #default="{ row }">{{ row.hotel || '--' }}</template>
          </el-table-column>
          <el-table-column prop="transportType" label="出行方式" min-width="130">
            <template #default="{ row }">{{ row.transportType || '--' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="220">
            <template #default="{ row }">{{ row.remark || '--' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditItemDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDeleteItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </SectionCard>

    <SectionCard title="行程流程图" description="上方步骤条看整体节奏，下方时间线看每天的具体安排。">
      <el-empty v-if="!planItems.length" description="新增每日安排后，这里会自动生成多日行程图。" />

      <template v-else>
        <div class="flow-panel">
          <el-steps :active="planItems.length" align-center finish-status="success">
            <el-step
              v-for="item in planItems"
              :key="item.id"
              :title="`第 ${item.dayNo} 天 · ${item.destination}`"
              :description="stepDescription(item)"
            />
          </el-steps>
        </div>

        <div class="timeline-panel">
          <el-timeline>
            <el-timeline-item
              v-for="item in planItems"
              :key="item.id"
              :timestamp="timelineTitle(item)"
              placement="top"
              type="primary"
            >
              <div class="timeline-card">
                <h4>{{ item.destination }}</h4>
                <p><strong>酒店：</strong>{{ item.hotel || '待定' }}</p>
                <p><strong>出行方式：</strong>{{ item.transportType || '待定' }}</p>
                <p><strong>备注：</strong>{{ item.remark || '暂无备注' }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
    </SectionCard>

    <el-dialog v-model="itemDialogVisible" :title="editingItemId ? '编辑每日安排' : '新增每日安排'" width="560px">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="100px">
        <el-form-item label="第几天" prop="dayNo">
          <el-input-number v-model="itemForm.dayNo" :min="1" :max="planForm.totalDays || 60" controls-position="right" />
        </el-form-item>
        <el-form-item label="目的地" prop="destination">
          <el-input v-model="itemForm.destination" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="酒店">
          <el-input v-model="itemForm.hotel" maxlength="100" show-word-limit placeholder="可选" />
        </el-form-item>
        <el-form-item label="出行方式">
          <el-select v-model="itemForm.transportType" placeholder="可选" clearable style="width: 100%;">
            <el-option label="飞机" value="飞机" />
            <el-option label="高铁" value="高铁" />
            <el-option label="火车" value="火车" />
            <el-option label="自驾" value="自驾" />
            <el-option label="打车" value="打车" />
            <el-option label="地铁" value="地铁" />
            <el-option label="步行" value="步行" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="itemForm.remark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="填写当天游玩重点、入住提醒、交通备注等"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingItem" @click="handleSaveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import SectionCard from '@/components/SectionCard.vue'
import {
  createTripPlanItem,
  deleteTripPlanItem,
  getTripPlanDetail,
  updateTripPlan,
  updateTripPlanItem
} from '@/api/tripPlan'

const route = useRoute()
const router = useRouter()
const planId = computed(() => route.params.id)
const loading = ref(false)
const savingPlan = ref(false)
const savingItem = ref(false)
const itemDialogVisible = ref(false)
const editingItemId = ref(null)
const planFormRef = ref()
const itemFormRef = ref()
const planItems = ref([])

const planForm = reactive({
  planName: '',
  totalDays: 1,
  startDate: '',
  remark: '',
  sourceType: 'MANUAL'
})

const itemForm = reactive({
  dayNo: 1,
  destination: '',
  hotel: '',
  transportType: '',
  remark: ''
})

const planRules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  totalDays: [{ required: true, message: '请输入总天数', trigger: 'change' }]
}

const itemRules = {
  dayNo: [{ required: true, message: '请输入第几天', trigger: 'change' }],
  destination: [{ required: true, message: '请输入目的地', trigger: 'blur' }]
}

async function loadDetail() {
  loading.value = true
  try {
    const response = await getTripPlanDetail(planId.value)
    const data = response.data
    planForm.planName = data.planName
    planForm.totalDays = data.totalDays
    planForm.startDate = data.startDate || ''
    planForm.remark = data.remark || ''
    planForm.sourceType = data.sourceType || 'MANUAL'
    planItems.value = (data.items || []).slice().sort((a, b) => a.dayNo - b.dayNo)
  } finally {
    loading.value = false
  }
}

function resetItemForm() {
  editingItemId.value = null
  itemForm.dayNo = nextSuggestedDay()
  itemForm.destination = ''
  itemForm.hotel = ''
  itemForm.transportType = ''
  itemForm.remark = ''
}

function nextSuggestedDay() {
  if (!planItems.value.length) {
    return 1
  }
  return Math.min(planForm.totalDays || 60, Math.max(...planItems.value.map((item) => item.dayNo)) + 1)
}

function openCreateItemDialog() {
  resetItemForm()
  itemDialogVisible.value = true
}

function openEditItemDialog(item) {
  editingItemId.value = item.id
  itemForm.dayNo = item.dayNo
  itemForm.destination = item.destination
  itemForm.hotel = item.hotel || ''
  itemForm.transportType = item.transportType || ''
  itemForm.remark = item.remark || ''
  itemDialogVisible.value = true
}

async function handleSavePlan() {
  const valid = await planFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  savingPlan.value = true
  try {
    await updateTripPlan(planId.value, {
      planName: planForm.planName,
      totalDays: planForm.totalDays,
      startDate: planForm.startDate || null,
      remark: planForm.remark || null
    })
    ElMessage.success('计划信息已保存')
    await loadDetail()
  } finally {
    savingPlan.value = false
  }
}

async function handleSaveItem() {
  const valid = await itemFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  savingItem.value = true
  try {
    const payload = {
      dayNo: itemForm.dayNo,
      destination: itemForm.destination,
      hotel: itemForm.hotel || null,
      transportType: itemForm.transportType || null,
      remark: itemForm.remark || null
    }
    if (editingItemId.value) {
      await updateTripPlanItem(planId.value, editingItemId.value, payload)
      ElMessage.success('每日安排已更新')
    } else {
      await createTripPlanItem(planId.value, payload)
      ElMessage.success('每日安排已新增')
    }
    itemDialogVisible.value = false
    await loadDetail()
  } finally {
    savingItem.value = false
  }
}

async function handleDeleteItem(item) {
  await ElMessageBox.confirm(`确认删除第 ${item.dayNo} 天的安排吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteTripPlanItem(planId.value, item.id)
  ElMessage.success('每日安排已删除')
  await loadDetail()
}

function stepDescription(item) {
  const parts = []
  if (item.transportType) {
    parts.push(item.transportType)
  }
  if (item.hotel) {
    parts.push(`入住 ${item.hotel}`)
  }
  if (item.remark) {
    parts.push(item.remark)
  }
  return parts.join(' | ') || '待补充当天安排'
}

function timelineTitle(item) {
  return `第 ${item.dayNo} 天`
}

function goBack() {
  router.push('/trip-plans')
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.trip-plan-detail-page {
  display: grid;
  gap: 24px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.plan-form :deep(.el-input-number) {
  width: 100%;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.flow-panel {
  padding: 20px 16px 8px;
  margin-bottom: 24px;
  border-radius: 18px;
  background: linear-gradient(180deg, #f7fbff 0%, #ffffff 100%);
  border: 1px solid #e3edf8;
}

.timeline-panel {
  padding: 8px 4px 0;
}

.timeline-card {
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid #e7edf5;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.timeline-card h4 {
  margin: 0 0 10px;
  font-size: 17px;
  color: #12314d;
}

.timeline-card p {
  margin: 6px 0;
  color: #41556f;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .flow-panel {
    overflow-x: auto;
  }
}
</style>
