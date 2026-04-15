<template>
  <div class="trip-plan-page">
    <SectionCard title="行程规划系统" description="创建多日旅行计划，维护每日安排，并从详情页查看可视化行程步骤图。">
      <template #extra>
        <el-button type="primary" @click="openCreateDialog">新建旅行计划</el-button>
      </template>

      <el-empty v-if="!loading && !plans.length" description="还没有旅行计划，先创建一个吧。" />

      <div v-else class="plan-grid" v-loading="loading">
        <article v-for="plan in plans" :key="plan.id" class="plan-card">
          <div class="plan-card__head">
            <div>
              <h3>{{ plan.planName }}</h3>
              <p>{{ formatDate(plan.startDate) }} · {{ plan.totalDays }} 天 · 已安排 {{ plan.itemCount }} 天</p>
            </div>
            <el-tag size="small" type="success">{{ sourceTypeText(plan.sourceType) }}</el-tag>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import SectionCard from '@/components/SectionCard.vue'
import { createTripPlan, deleteTripPlan, getTripPlanList, updateTripPlan } from '@/api/tripPlan'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const planFormRef = ref()
const plans = ref([])

const planForm = reactive({
  planName: '',
  totalDays: 3,
  startDate: '',
  remark: ''
})

const planRules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  totalDays: [{ required: true, message: '请输入总天数', trigger: 'change' }]
}

function resetForm() {
  editingId.value = null
  planForm.planName = ''
  planForm.totalDays = 3
  planForm.startDate = ''
  planForm.remark = ''
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
</style>
