<template>
  <div class="admin-page">
    <section class="admin-card">
      <div class="admin-section__head">
        <div>
          <h2>用户管理</h2>
          <p>支持搜索用户、启停账号和分配管理员角色。</p>
        </div>
      </div>

      <div class="admin-toolbar">
        <el-input v-model="query.keyword" placeholder="用户名 / 昵称 / 手机号" clearable class="admin-filter" />
        <el-select v-model="query.status" placeholder="状态" clearable class="admin-filter">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="loadUsers">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table :data="users" border v-loading="loading">
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag v-for="role in row.roleCodes" :key="role" :type="role === 'ROLE_ADMIN' ? 'danger' : 'info'">
                {{ role }}
              </el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最近登录" min-width="180" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRoleDialog(row)">角色设置</el-button>
            <el-button link type="warning" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link @click="openDetailDialog(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <el-dialog v-model="roleDialogVisible" title="角色设置" width="480px">
      <el-checkbox-group v-model="selectedRoles">
        <el-checkbox v-for="role in roles" :key="role.roleCode" :label="role.roleCode">
          {{ role.roleName }}（{{ role.roleCode }}）
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="用户详情" width="600px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ detail.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '启用' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="角色" :span="2">{{ (detail.roleCodes || []).join(', ') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近登录" :span="2">{{ detail.lastLoginTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAdminRoles,
  getAdminUserDetail,
  getAdminUsers,
  updateAdminUserRoles,
  updateAdminUserStatus
} from '@/api/admin'

const loading = ref(false)
const roleSaving = ref(false)
const users = ref([])
const total = ref(0)
const roles = ref([])
const selectedRoles = ref([])
const currentUserId = ref(null)
const roleDialogVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)

const query = reactive({
  keyword: '',
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

async function loadUsers() {
  loading.value = true
  try {
    const response = await getAdminUsers(query)
    users.value = response.data.records || []
    total.value = response.data.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { keyword: '', status: undefined, pageNum: 1, pageSize: 10 })
  loadUsers()
}

async function openRoleDialog(row) {
  currentUserId.value = row.id
  selectedRoles.value = [...row.roleCodes]
  roleDialogVisible.value = true
}

async function saveRoles() {
  roleSaving.value = true
  try {
    await updateAdminUserRoles(currentUserId.value, { roleCodes: selectedRoles.value })
    ElMessage.success('角色更新成功')
    roleDialogVisible.value = false
    loadUsers()
  } finally {
    roleSaving.value = false
  }
}

async function toggleStatus(row) {
  await updateAdminUserStatus(row.id, { status: row.status === 1 ? 0 : 1 })
  ElMessage.success('用户状态已更新')
  loadUsers()
}

async function openDetailDialog(row) {
  const response = await getAdminUserDetail(row.id)
  detail.value = response.data
  detailVisible.value = true
}

function handlePageChange(page) {
  query.pageNum = page
  loadUsers()
}

onMounted(async () => {
  const roleResponse = await getAdminRoles()
  roles.value = roleResponse.data || []
  loadUsers()
})
</script>
