<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand" @click="$router.push('/')">
        <span class="brand-badge">TP</span>
        <div>
          <h1>出行旅游平台</h1>
          <p>机票、火车票、酒店、度假和旅行分享的一站式体验</p>
        </div>
      </div>

      <nav class="nav-list">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/flight">机票</RouterLink>
        <RouterLink to="/train">火车票</RouterLink>
        <RouterLink to="/hotel">酒店</RouterLink>
        <RouterLink to="/tour">旅游</RouterLink>
        <RouterLink to="/shares">分享</RouterLink>
        <RouterLink to="/trip-plans">行程规划</RouterLink>
        <RouterLink to="/orders">订单</RouterLink>
      </nav>

      <div class="topbar-actions">
        <template v-if="userStore.isLoggedIn">
          <span class="welcome-text">你好，{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</span>
          <el-button type="primary" plain @click="$router.push('/shares/create')">发布分享</el-button>
          <el-button v-if="userStore.isAdmin" type="primary" plain @click="$router.push('/admin/dashboard')">后台管理</el-button>
          <el-button plain @click="$router.push('/profile')">个人中心</el-button>
          <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
        </template>
        <template v-else>
          <el-button plain @click="$router.push('/shares')">浏览分享</el-button>
          <el-button type="primary" @click="$router.push('/login')">登录 / 注册</el-button>
        </template>
      </div>
    </header>

    <main class="page-container">
      <RouterView />
    </main>
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
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(43, 123, 217, 0.12), transparent 28%),
    linear-gradient(180deg, #f6faff 0%, #f4f7fb 100%);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 28px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(18, 49, 77, 0.08);
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
}

.brand h1 {
  margin: 0;
  font-size: 20px;
  color: #12314d;
}

.brand p {
  margin: 4px 0 0;
  color: #66758d;
  font-size: 13px;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #0f5fa8 0%, #39a0ed 100%);
  color: #fff;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.nav-list {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

.nav-list a {
  color: #38506b;
  text-decoration: none;
  font-weight: 500;
}

.nav-list a.router-link-active {
  color: #0f5fa8;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.welcome-text {
  color: #38506b;
  font-size: 14px;
}

.page-container {
  width: min(1280px, calc(100% - 32px));
  margin: 0 auto;
  padding: 28px 0 40px;
}

@media (max-width: 1100px) {
  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .topbar-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
