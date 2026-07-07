<template>
  <div class="auth-page">
    <div class="login-animation">
      <div class="animation-stage">
        <img :src="logoUrl" alt="MF" class="stage-logo" />
      </div>
      <div class="animation-label">MF Merchant</div>
    </div>
    <div class="auth-panel">
      <div class="auth-brand brand-lockup">
        <span class="brand-logo-box"><img :src="logoUrl" alt="MF" class="brand-logo" /></span>
      </div>
      <h1>苗丰商家端</h1>
      <el-form :model="form" size="large" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit-btn" @click="handleLogin">登录</el-button>
      </el-form>
      <div class="auth-link">还没有账号？<router-link to="/register">提交入驻申请</router-link></div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginMerchant } from '@/api/merchant'
import logoUrl from '@/assets/brand/logo.png'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  loading.value = true
  try {
    const res = await loginMerchant(form)
    localStorage.setItem('merchantToken', res.data.token)
    localStorage.setItem('merchantInfo', JSON.stringify(res.data))
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  height: 100vh;
  background: var(--color-bg);
  overflow: hidden;
}

.login-animation {
  flex: 1;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-soft);
  border-right: 1px solid var(--color-border-strong);
}

.animation-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 260px;
  height: 260px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-panel);
  background: var(--color-surface);
  box-shadow: var(--shadow-panel);
}

.stage-logo {
  width: 132px;
  height: 132px;
  object-fit: contain;
}

.animation-label {
  margin-top: 26px;
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: 0;
}

.auth-panel {
  width: 380px;
  margin: 0 50px;
  padding: 28px;
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
  height: 44px;
  font-size: 15px;
  letter-spacing: 0;
}

.auth-link {
  margin-top: 18px;
  text-align: center;
  color: var(--color-text-secondary);
}

.auth-link a {
  color: var(--color-primary);
}

@media (max-width: 900px) {
  .auth-page {
    min-height: 100vh;
    height: auto;
    flex-direction: column;
  }

  .login-animation {
    flex: none;
    width: 100%;
    min-height: 360px;
    border-right: 0;
    border-bottom: 1px solid var(--color-border-strong);
  }

  .animation-stage {
    width: 200px;
    height: 200px;
  }

  .stage-logo {
    width: 104px;
    height: 104px;
  }

  .auth-panel {
    width: min(380px, calc(100% - 32px));
    margin: 28px 16px;
  }
}
</style>
