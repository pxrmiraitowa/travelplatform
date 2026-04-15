<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand" @click="$router.push('/admin/dashboard')">
        <span class="admin-brand__badge">AM</span>
        <div>
          <strong>后台管理系统</strong>
          <p>Travel Platform Admin</p>
        </div>
      </div>

      <el-menu :default-active="$route.path" class="admin-menu" router>
        <el-menu-item index="/admin/dashboard">控制台</el-menu-item>
        <el-menu-item index="/admin/users">用户管理</el-menu-item>
        <el-sub-menu index="product-group">
          <template #title>产品管理</template>
          <el-menu-item index="/admin/products/flights">航班管理</el-menu-item>
          <el-menu-item index="/admin/products/trains">车次管理</el-menu-item>
          <el-menu-item index="/admin/products/hotels">酒店管理</el-menu-item>
          <el-menu-item index="/admin/products/rooms">房型管理</el-menu-item>
          <el-menu-item index="/admin/products/tours">旅游产品</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/admin/orders">订单管理</el-menu-item>
        <el-sub-menu index="content-group">
          <template #title>内容管理</template>
          <el-menu-item index="/admin/content/shares">分享管理</el-menu-item>
          <el-menu-item index="/admin/content/reviews">评价管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>

    <section class="admin-main">
      <header class="admin-topbar">
        <div>
          <h1>{{ $route.meta?.title || '后台管理' }}</h1>
          <p>统一维护用户、产品、订单和社区内容数据</p>
        </div>

        <div class="admin-topbar__actions">
          <span class="admin-user">
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员' }}
          </span>
          <el-button plain @click="$router.push('/')">返回前台</el-button>
          <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
        </div>
      </header>

      <main class="admin-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

async function handleLogout() {
  await userStore.logoutAction()
  ElMessage.success('已退出后台登录')
  router.push('/admin/login')
}
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
  background: #f4f7fb;
}

.admin-sidebar {
  padding: 20px 16px;
  background: #0f2438;
  color: #fff;
}

.admin-brand {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
  cursor: pointer;
}

.admin-brand p {
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.admin-brand__badge {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: linear-gradient(135deg, #1d8cf8 0%, #43c6ac 100%);
  font-weight: 700;
}

.admin-menu {
  border-right: none;
  background: transparent;
}

.admin-main {
  min-width: 0;
}

.admin-topbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e6ebf2;
  background: rgba(255, 255, 255, 0.92);
}

.admin-topbar h1 {
  margin: 0;
  font-size: 24px;
}

.admin-topbar p {
  margin: 6px 0 0;
  color: #6b7280;
}

.admin-topbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.admin-user {
  color: #334155;
  font-weight: 600;
}

.admin-content {
  padding: 24px;
}

@media (max-width: 960px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }
}
</style>
