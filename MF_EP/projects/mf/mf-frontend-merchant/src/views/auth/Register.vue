<template>
  <div class="auth-page">
    <div class="auth-panel">
      <div class="auth-brand brand-lockup">
        <span class="brand-logo-box"><img :src="logoUrl" alt="MF" class="brand-logo" /></span>
      </div>
      <h1>商家入驻申请</h1>
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="店铺名称"><el-input v-model="form.shopName" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-button type="primary" :loading="loading" class="submit-btn" @click="handleRegister">提交申请</el-button>
      </el-form>
      <div class="auth-link">已有账号？<router-link to="/login">返回登录</router-link></div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerMerchant } from '@/api/merchant'
import logoUrl from '@/assets/brand/logo.png'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '', shopName: '', contactName: '', phone: '' })

const handleRegister = async () => {
  loading.value = true
  try {
    await registerMerchant(form)
    ElMessage.success('申请已提交，请等待平台审核')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(180deg, #f7fbf6 0%, var(--color-bg) 48%, #edf5ed 100%);
}

.auth-panel {
  width: 440px;
  padding: 32px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-panel);
  box-shadow: var(--shadow-panel);
}

.auth-brand {
  justify-content: center;
  margin-bottom: 18px;
}

h1 {
  margin-bottom: 24px;
  font-size: 24px;
  color: var(--color-primary-dark);
  text-align: center;
}

.submit-btn {
  width: 100%;
}

.auth-link {
  margin-top: 18px;
  text-align: center;
  color: var(--color-text-secondary);
}

.auth-link a {
  color: var(--color-primary);
}
</style>
