<template>
  <div class="trip-plan-page">
    <SectionCard
      title="行程规划系统"
      description="创建多日旅行计划，维护每日安排，也可以借助 AI 先生成景点预览，再一键保存到我的行程规划。"
    >
      <template #extra>
        <div class="header-actions">
          <el-button @click="openAiDialog">AI 生成行程</el-button>
          <el-button type="primary" @click="openCreateDialog">新建旅行计划</el-button>
        </div>
      </template>

      <el-empty v-if="!loading && !plans.length" description="还没有旅行计划，先创建一个吧。" />

      <div v-else class="plan-grid" v-loading="loading">
        <article v-for="plan in plans" :key="plan.id" class="plan-card">
          <div class="plan-card__head">
            <div>
              <h3>{{ plan.planName }}</h3>
              <p>{{ formatDate(plan.startDate) }} · {{ plan.totalDays }} 天 · 已安排 {{ plan.itemCount }} 天</p>
            </div>
            <el-tag size="small" :type="plan.sourceType === 'AI' ? 'warning' : 'success'">
              {{ sourceTypeText(plan.sourceType) }}
            </el-tag>
          </div>

          <p class="plan-card__remark">{{ plan.remark || '暂无整体备注，可进入详情页继续补充。' }}</p>

          <div class="plan-card__meta">
            <span>创建时间：{{ formatDateTime(plan.createTime) }}</span>
          </div>

          <div class="plan-card__actions">
            <el-button type="primary" @click="goDetail(plan.id)">查看详情</el-button>
            <el-button @click="openEditDialog(plan)">编辑计划</el-button>
            <el-button type="danger" plain @click="handleDelete(plan)">删除</el-button>
          </div>
        </article>
      </div>
    </SectionCard>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑旅行计划' : '新建旅行计划'" width="520px">
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="100px">
        <el-form-item label="计划名称" prop="planName">
          <el-input v-model="planForm.planName" placeholder="例如：北京 5 日游" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="总天数" prop="totalDays">
          <el-input-number v-model="planForm.totalDays" :min="1" :max="60" controls-position="right" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker
            v-model="planForm.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="可选"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="整体备注">
          <el-input
            v-model="planForm.remark"
            type="textarea"
            :rows="3"
            placeholder="填写出行主题、同行人、预算提醒等"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiDialogVisible" title="AI 生成行程" width="920px" destroy-on-close class="ai-dialog">
      <div class="ai-planner">
        <el-form ref="aiFormRef" :model="aiForm" :rules="aiRules" label-width="100px" class="ai-form">
          <el-row :gutter="16">
            <el-col :md="12" :xs="24">
              <el-form-item label="目的地" prop="destination">
                <el-select v-model="aiForm.destination" placeholder="请选择目的地" filterable style="width: 100%;">
                  <el-option
                    v-for="city in destinationOptions"
                    :key="city"
                    :label="city"
                    :value="city"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="停留天数" prop="totalDays">
                <el-input-number v-model="aiForm.totalDays" :min="1" :max="10" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="出发日期">
                <el-date-picker
                  v-model="aiForm.startDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="可选"
                  style="width: 100%;"
                />
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="旅游偏好">
                <el-select
                  v-model="aiForm.preferences"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="可多选"
                  style="width: 100%;"
                >
                  <el-option
                    v-for="preference in preferenceOptions"
                    :key="preference"
                    :label="preference"
                    :value="preference"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="ai-toolbar">
          <el-button type="primary" :loading="aiGenerating" @click="handleGeneratePreview">生成预览</el-button>
        </div>

        <div v-if="aiPreview" class="ai-preview">
          <div class="preview-head">
            <div>
              <h3>{{ aiPreview.planName }}</h3>
              <p>{{ aiPreview.destination }} · {{ aiPreview.totalDays }} 天 · {{ previewPreferenceText }}</p>
            </div>
          </div>

          <div class="preview-days">
            <article v-for="day in aiPreview.days" :key="day.dayNo" class="preview-day-card">
              <div class="preview-day-head">
                <div>
                  <strong>Day {{ day.dayNo }}</strong>
                  <h4>{{ day.destination }}</h4>
                </div>
              </div>

              <p class="preview-reason">{{ day.reason }}</p>

              <ul class="preview-attractions">
                <li v-for="attraction in day.attractions" :key="attraction.id">
                  <div class="preview-attraction-title">
                    <span>{{ attraction.attractionName }}</span>
                    <small>{{ attraction.suggestedDuration || '建议半日游' }}</small>
                  </div>
                  <p>{{ attraction.description }}</p>
                  <div class="preview-tags">
                    <el-tag
                      v-for="tag in attraction.tags || []"
                      :key="`${attraction.id}-${tag}`"
                      size="small"
                      effect="plain"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                </li>
              </ul>
            </article>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!aiPreview" :loading="aiSaving" @click="handleSaveAiPlan">
          一键保存到行程规划
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import SectionCard from '@/components/SectionCard.vue'
import {
  createTripPlan,
  deleteTripPlan,
  getTripPlanList,
  previewAiTripPlan,
  saveAiTripPlan,
  updateTripPlan
} from '@/api/tripPlan'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const aiDialogVisible = ref(false)
const editingId = ref(null)
const planFormRef = ref()
const aiFormRef = ref()
const plans = ref([])
const aiGenerating = ref(false)
const aiSaving = ref(false)
const aiPreview = ref(null)

const destinationOptions = [
  '北京', '上海', '杭州', '成都', '西安', '重庆',
  '广州', '深圳', '南京', '苏州', '三亚', '桂林'
]

const preferenceOptions = ['自然风光', '人文历史', '美食', '亲子', '休闲', '城市地标']

const planForm = reactive({
  planName: '',
  totalDays: 3,
  startDate: '',
  remark: ''
})

const aiForm = reactive({
  destination: '',
  totalDays: 3,
  startDate: '',
  preferences: ['自然风光']
})

const planRules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  totalDays: [{ required: true, message: '请输入总天数', trigger: 'change' }]
}

const aiRules = {
  destination: [{ required: true, message: '请选择目的地', trigger: 'change' }],
  totalDays: [{ required: true, message: '请输入停留天数', trigger: 'change' }]
}

const previewPreferenceText = computed(() => {
  const preferences = aiPreview.value?.preferences || []
  return preferences.length ? preferences.join(' / ') : '综合推荐'
})

function resetForm() {
  editingId.value = null
  planForm.planName = ''
  planForm.totalDays = 3
  planForm.startDate = ''
  planForm.remark = ''
}

function resetAiPreview() {
  aiPreview.value = null
}

async function loadPlans() {
  loading.value = true
  try {
    const response = await getTripPlanList()
    plans.value = response.data || []
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openAiDialog() {
  resetAiPreview()
  aiDialogVisible.value = true
}

function openEditDialog(plan) {
  editingId.value = plan.id
  planForm.planName = plan.planName
  planForm.totalDays = plan.totalDays
  planForm.startDate = plan.startDate || ''
  planForm.remark = plan.remark || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await planFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const payload = {
      planName: planForm.planName,
      totalDays: planForm.totalDays,
      startDate: planForm.startDate || null,
      remark: planForm.remark || null
    }
    if (editingId.value) {
      await updateTripPlan(editingId.value, payload)
      ElMessage.success('旅行计划已更新')
    } else {
      await createTripPlan(payload)
      ElMessage.success('旅行计划已创建')
    }
    dialogVisible.value = false
    await loadPlans()
  } finally {
    submitting.value = false
  }
}

async function handleGeneratePreview() {
  const valid = await aiFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  aiGenerating.value = true
  try {
    const response = await previewAiTripPlan({
      destination: aiForm.destination,
      totalDays: aiForm.totalDays,
      startDate: aiForm.startDate || null,
      preferences: aiForm.preferences
    })
    aiPreview.value = response.data
    ElMessage.success('AI 行程预览已生成')
  } finally {
    aiGenerating.value = false
  }
}

async function handleSaveAiPlan() {
  if (!aiPreview.value) {
    return
  }

  aiSaving.value = true
  try {
    const response = await saveAiTripPlan({
      planName: aiPreview.value.planName,
      destination: aiPreview.value.destination,
      totalDays: aiPreview.value.totalDays,
      startDate: aiPreview.value.startDate || null,
      preferences: aiPreview.value.preferences || [],
      days: (aiPreview.value.days || []).map((day) => ({
        dayNo: day.dayNo,
        attractionIds: (day.attractions || []).map((attraction) => attraction.id),
        reason: day.reason
      }))
    })
    ElMessage.success('AI 行程已保存到行程规划')
    aiDialogVisible.value = false
    resetAiPreview()
    await loadPlans()
    router.push(`/trip-plans/${response.data.id}`)
  } finally {
    aiSaving.value = false
  }
}

function goDetail(id) {
  router.push(`/trip-plans/${id}`)
}

async function handleDelete(plan) {
  await ElMessageBox.confirm(`确认删除旅行计划“${plan.planName}”吗？其每日安排也会一并删除。`, '删除确认', {
    type: 'warning'
  })
  await deleteTripPlan(plan.id)
  ElMessage.success('旅行计划已删除')
  await loadPlans()
}

function formatDate(value) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '未设置出发日期'
}

function formatDateTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--'
}

function sourceTypeText(value) {
  if (value === 'AI') {
    return 'AI 生成'
  }
  return value === 'MANUAL' ? '手动规划' : value
}

onMounted(() => {
  loadPlans()
})
</script>

<style scoped>
.trip-plan-page {
  display: grid;
  gap: 24px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.plan-card {
  display: grid;
  gap: 14px;
  padding: 20px;
  border: 1px solid #e7edf5;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.04);
}

.plan-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.plan-card__head h3 {
  margin: 0 0 6px;
  font-size: 18px;
  color: #12314d;
}

.plan-card__head p {
  margin: 0;
  color: #6b7a90;
  font-size: 13px;
}

.plan-card__remark {
  margin: 0;
  min-height: 44px;
  color: #3f536e;
  line-height: 1.7;
}

.plan-card__meta {
  color: #7a869a;
  font-size: 12px;
}

.plan-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ai-planner {
  display: grid;
  gap: 18px;
}

.ai-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.ai-preview {
  display: grid;
  gap: 18px;
}

.preview-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  padding: 20px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f8fcff 0%, #eef6ff 100%);
  border: 1px solid #dce9f8;
}

.preview-head h3 {
  margin: 0 0 6px;
  color: #12314d;
}

.preview-head p {
  margin: 0;
  color: #607086;
}

.preview-days {
  display: grid;
  gap: 16px;
}

.preview-day-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 18px;
  border: 1px solid #e4ebf5;
  background: #fff;
}

.preview-day-head strong {
  color: #125fba;
  font-size: 14px;
}

.preview-day-head h4 {
  margin: 6px 0 0;
  font-size: 20px;
  color: #12314d;
}

.preview-reason {
  margin: 0;
  color: #4a6078;
  line-height: 1.8;
}

.preview-attractions {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.preview-attractions li {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fbff;
  border: 1px solid #e8f0fb;
}

.preview-attraction-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
  color: #10243a;
  font-weight: 700;
}

.preview-attraction-title small {
  color: #6b7a90;
  font-weight: 500;
}

.preview-attractions p {
  margin: 0;
  color: #506277;
  line-height: 1.75;
}

.preview-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

@media (max-width: 768px) {
  .preview-head,
  .preview-attraction-title {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
