<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminLogin } from '@/api/admin'

const router = useRouter()
const isSubmitting = ref(false)
const error = ref('')
const loginForm = ref({ username: '', password: '' })

async function handleLogin() {
  if (!loginForm.value.username.trim() || !loginForm.value.password.trim()) {
    error.value = '请输入管理员账号和密码'
    return
  }
  isSubmitting.value = true
  error.value = ''
  try {
    await adminLogin(loginForm.value.username.trim(), loginForm.value.password)
    loginForm.value.password = ''
    router.push('/admin/dashboard')
  } catch (err: any) {
    error.value = err.response?.data?.message || '登录失败'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">💬</div>
        <h1>管理后台</h1>
        <p>请登录管理员账号</p>
      </div>

      <el-form @submit.prevent="handleLogin" class="login-form">
        <el-form-item>
          <el-input
            v-model="loginForm.username"
            placeholder="管理员账号"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            size="large"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-alert
          v-if="error"
          :title="error"
          type="error"
          show-icon
          :closable="false"
          class="login-error"
        />

        <el-button
          type="primary"
          size="large"
          :loading="isSubmitting"
          class="login-btn"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0f0f0f;
}

.login-card {
  width: 400px;
  padding: 48px 40px;
  background: #1f1f1f;
  border-radius: 16px;
  border: 1px solid #2a2a2a;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-logo {
  font-size: 48px;
  margin-bottom: 16px;
}

.login-header h1 {
  color: #ffffff;
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}

.login-header p {
  color: #6b7280;
  font-size: 14px;
}

.login-form {
  margin-top: 24px;
}

.login-error {
  margin-bottom: 16px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}
</style>
