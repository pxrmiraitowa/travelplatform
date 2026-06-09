<template>
  <div class="profile-page">
    <SectionCard title="个人资料" description="查看并维护当前账号的基础资料。">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="100px" class="profile-form">
        <el-row :gutter="20">
          <el-col :md="12" :xs="24">
            <el-form-item label="用户名">
              <el-input :model-value="userStore.userInfo?.username" disabled />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="性别">
              <el-select v-model="profileForm.gender" placeholder="请选择性别" clearable>
                <el-option label="未知" :value="0" />
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="profile-actions">
          <el-button type="primary" :loading="profileLoading" @click="handleSaveProfile">保存资料</el-button>
        </div>
      </el-form>
    </SectionCard>

    <SectionCard title="常用出行人" description="管理下单时常用的联系人信息。">
      <div class="contact-toolbar">
        <el-button type="primary" @click="openCreateDialog">新增出行人</el-button>
      </div>

      <el-table :data="contacts" border v-loading="contactsLoading">
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column prop="phone" label="联系电话" min-width="140" />
        <el-table-column prop="idCard" label="身份证号" min-width="180" />
        <el-table-column label="类型" min-width="100">
          <template #default="{ row }">
            {{ row.contactType === 2 ? '儿童' : '成人' }}
          </template>
        </el-table-column>
        <el-table-column label="默认" min-width="90">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success">默认</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDeleteContact(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </SectionCard>

    <SectionCard title="价格提醒" description="查看已关注产品的当前价格与提醒状态，状态会根据当前数据库价格动态计算。">
      <el-table :data="priceAlerts" border v-loading="priceAlertsLoading">
        <el-table-column prop="productName" label="产品名称" min-width="180" />
        <el-table-column label="类型" min-width="100">
          <template #default="{ row }">
            {{ formatProductType(row.productType) }}
          </template>
        </el-table-column>
        <el-table-column label="当前价格" min-width="120">
          <template #default="{ row }">￥{{ Number(row.currentPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="目标价格" min-width="120">
          <template #default="{ row }">￥{{ Number(row.targetPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="140">
          <template #default="{ row }">
            <el-tag :type="row.triggered ? 'success' : 'warning'">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDeletePriceAlert(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </SectionCard>

    <el-dialog v-model="contactDialogVisible" :title="editingContactId ? '编辑常用出行人' : '新增常用出行人'" width="520px">
      <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="contactForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="contactForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="contactForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="contactForm.contactType">
            <el-option label="成人" :value="1" />
            <el-option label="儿童" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认联系人">
          <el-switch v-model="contactForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="contactForm.remark" type="textarea" :rows="3" placeholder="可填写补充说明" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="contactDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="contactSubmitting" @click="handleSubmitContact">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SectionCard from '@/components/SectionCard.vue'
import { useUserStore } from '@/stores/user'
import { deletePriceAlert, getPriceAlerts } from '@/api/priceAlert'
import { createUserContact, deleteUserContact, getUserContacts, updateUserContact } from '@/api/userContact'

const userStore = useUserStore()
const profileFormRef = ref()
const contactFormRef = ref()
const profileLoading = ref(false)
const contactsLoading = ref(false)
const priceAlertsLoading = ref(false)
const contactSubmitting = ref(false)
const contactDialogVisible = ref(false)
const editingContactId = ref(null)
const contacts = ref([])
const priceAlerts = ref([])

const profileForm = reactive({
  nickname: '',
  realName: '',
  phone: '',
  email: '',
  gender: null,
  avatar: ''
})

const contactForm = reactive({
  name: '',
  phone: '',
  idCard: '',
  contactType: 1,
  isDefault: 0,
  remark: ''
})

const profileRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1\\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }]
}

const contactRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }]
}

profileRules.phone = [{ pattern: /^$|^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }]
contactRules.phone = [
  { required: true, message: '请输入联系电话', trigger: 'blur' },
  { pattern: /^1\d{10}$/, message: '请输入正确的联系电话', trigger: 'blur' }
]

function syncProfileForm() {
  const userInfo = userStore.userInfo || {}
  profileForm.nickname = userInfo.nickname || ''
  profileForm.realName = userInfo.realName || ''
  profileForm.phone = userInfo.phone || ''
  profileForm.email = userInfo.email || ''
  profileForm.gender = userInfo.gender ?? null
  profileForm.avatar = userInfo.avatar || ''
}

function resetContactForm() {
  editingContactId.value = null
  contactForm.name = ''
  contactForm.phone = ''
  contactForm.idCard = ''
  contactForm.contactType = 1
  contactForm.isDefault = 0
  contactForm.remark = ''
}

async function loadContacts() {
  contactsLoading.value = true
  try {
    const response = await getUserContacts()
    contacts.value = response.data
  } finally {
    contactsLoading.value = false
  }
}

async function loadPriceAlerts() {
  priceAlertsLoading.value = true
  try {
    const response = await getPriceAlerts()
    priceAlerts.value = response.data
  } finally {
    priceAlertsLoading.value = false
  }
}

function openCreateDialog() {
  resetContactForm()
  contactDialogVisible.value = true
}

function openEditDialog(row) {
  editingContactId.value = row.id
  contactForm.name = row.name
  contactForm.phone = row.phone
  contactForm.idCard = row.idCard
  contactForm.contactType = row.contactType
  contactForm.isDefault = row.isDefault
  contactForm.remark = row.remark || ''
  contactDialogVisible.value = true
}

async function handleSaveProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  profileLoading.value = true
  try {
    await userStore.updateProfile(profileForm)
    ElMessage.success('个人资料已更新')
    syncProfileForm()
  } finally {
    profileLoading.value = false
  }
}

async function handleSubmitContact() {
  const valid = await contactFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  contactSubmitting.value = true
  try {
    if (editingContactId.value) {
      await updateUserContact(editingContactId.value, contactForm)
      ElMessage.success('常用出行人已更新')
    } else {
      await createUserContact(contactForm)
      ElMessage.success('常用出行人已新增')
    }
    contactDialogVisible.value = false
    await loadContacts()
  } finally {
    contactSubmitting.value = false
  }
}

async function handleDeleteContact(row) {
  await ElMessageBox.confirm(`确认删除出行人“${row.name}”吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteUserContact(row.id)
  ElMessage.success('常用出行人已删除')
  await loadContacts()
}

async function handleDeletePriceAlert(row) {
  await ElMessageBox.confirm(`确认删除“${row.productName}”的价格提醒吗？`, '删除确认', {
    type: 'warning'
  })
  await deletePriceAlert(row.id)
  ElMessage.success('价格提醒已删除')
  await loadPriceAlerts()
}

function formatProductType(type) {
  return {
    HOTEL: '酒店',
    FLIGHT: '航班',
    TOUR: '旅游产品'
  }[type] || type
}

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchCurrentUser()
  }
  syncProfileForm()
  await loadContacts()
  await loadPriceAlerts()
})
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 24px;
}

.profile-form :deep(.el-select) {
  width: 100%;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
}

.contact-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
</style>
