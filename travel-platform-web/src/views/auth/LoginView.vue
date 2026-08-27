<template>
  <div class="auth-page">
    <div class="auth-card">
      <button
        type="button"
        class="auth-card__close"
        aria-label="返回登录前页面"
        title="返回登录前页面"
        @click="handleClose"
      >
        <span aria-hidden="true">×</span>
      </button>

      <div class="auth-intro">
        <span class="hero-tag">账户中心</span>
        <h2>登录或注册出行旅游平台</h2>
        <p>完成账户认证后，可以查看个人资料、维护常用出行人，并接入后续订单与出行服务。</p>
      </div>

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @keyup.enter="handleLogin">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>

            <el-button type="primary" :loading="loginLoading" class="full-width" @click="handleLogin">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="4-20位字母、数字或下划线" />
            </el-form-item>

            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="请输入昵称" />
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
            </el-form-item>

            <el-button type="primary" :loading="registerLoading" class="full-width" @click="handleRegister">
              注册并登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loginFormRef = ref()
const registerFormRef = ref()
const loginLoading = ref(false)
const registerLoading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  nickname: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请输入确认密码', trigger: 'blur' }]
}

function handleClose() {
  if (window.history.state?.back) {
    router.back()
    return
  }

  router.push('/')
}

function handleLoginSuccess(payload, successMessage) {
  userStore.setLogin(payload.token, payload.userInfo)
  ElMessage.success(successMessage)
  router.push(route.query.redirect || '/profile')
}

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loginLoading.value = true
  try {
    const response = await login(loginForm)
    handleLoginSuccess(response.data, '登录成功')
  } finally {
    loginLoading.value = false
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  registerLoading.value = true
  try {
    const response = await register(registerForm)
    handleLoginSuccess(response.data, '注册成功')
  } finally {
    registerLoading.value = false
  }
}
</script>
