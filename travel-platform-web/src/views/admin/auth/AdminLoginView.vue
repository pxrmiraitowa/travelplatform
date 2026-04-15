<template>
  <div class="admin-login">
    <div class="admin-login__card">
      <div class="admin-login__intro">
        <span class="hero-tag">Admin Portal</span>
        <h2>后台管理系统登录</h2>
        <p>管理员可在这里统一维护用户、库存产品和订单数据。</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入管理员用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>

        <el-button class="full-width" type="primary" :loading="loading" @click="handleLogin">
          登录后台
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLogin } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    const response = await adminLogin(form)
    userStore.setLogin(response.data.token, response.data.userInfo)
    ElMessage.success('后台登录成功')
    router.push(route.query.redirect || '/admin/dashboard')
  } finally {
    loading.value = false
  }
}
</script>
