<template>
  <div class="login-page">
    <div class="login-banner">
      <div class="banner-content">
        <div class="login-brand brand-lockup">
          <span class="brand-logo-box"><img :src="logoUrl" alt="MF" class="brand-logo" /></span>
        </div>
        <div class="animation-stage">
          <img :src="logoUrl" alt="MF" class="stage-logo" />
        </div>
        <h1>🌱 苗丰施肥管控平台</h1>
        <p>科学施肥，智慧种植。为每一棵树找到最适合的养护方案。</p>
        <div class="banner-features">
          <div class="feature"><span>🌳</span> 树苗商城</div>
          <div class="feature"><span>🧪</span> 化肥商城</div>
          <div class="feature"><span>📚</span> 树木百科</div>
          <div class="feature"><span>🤖</span> 智能推荐</div>
        </div>
      </div>
    </div>
    <div class="login-form-area">
      <div class="form-card">
        <el-tabs v-model="activeTab" class="login-tabs">
          <el-tab-pane label="账号登录" name="login">
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" size="large">
              <el-form-item prop="username">
                <el-input v-model="loginForm.username" placeholder="用户名/手机号" prefix-icon="User" />
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="loading" class="submit-btn" @click="handleLogin">登 录</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="手机注册" name="register">
            <el-form ref="regFormRef" :model="regForm" :rules="regRules" size="large">
              <el-form-item prop="username">
                <el-input v-model="regForm.username" placeholder="设置用户名" prefix-icon="User" />
              </el-form-item>
              <el-form-item prop="phone">
                <el-input v-model="regForm.phone" placeholder="手机号" prefix-icon="Phone" />
              </el-form-item>
              <el-form-item prop="code">
                <el-input v-model="regForm.code" placeholder="验证码" prefix-icon="Message">
                  <template #append>
                    <el-button :disabled="codeCountdown > 0" @click="sendRegCode">
                      {{ codeCountdown > 0 ? codeCountdown + 's' : '获取验证码' }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="regForm.password" type="password" placeholder="设置密码" prefix-icon="Lock" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="loading" class="submit-btn" @click="handleRegister">注 册</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { login, register, sendCode } from '@/api/auth'
import logoUrl from '@/assets/brand/logo.png'

const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref('login')
const loading = ref(false)
const codeCountdown = ref(0)
const loginFormRef = ref(null)
const regFormRef = ref(null)

const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const regForm = reactive({ username: '', phone: '', code: '', password: '' })
const regRules = {
  username: [{ required: true, message: '设置用户名', trigger: 'blur' }],
  phone: [{ required: true, pattern: /^1\d{10}$/, message: '请输入有效手机号', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!await loginFormRef.value.validate().catch(() => false)) return
  loading.value = true
  try {
    const res = await login({ username: loginForm.username, password: loginForm.password })
    if (res.code === 200 && res.data) {
      authStore.setToken(res.data.token)
      authStore.setUserInfo(res.data)
      ElMessage.success('登录成功')
      router.push('/')
    }
  } catch {} finally { loading.value = false }
}

const handleRegister = async () => {
  if (!await regFormRef.value.validate().catch(() => false)) return
  loading.value = true
  try {
    await register({ username: regForm.username, phone: regForm.phone, code: regForm.code, password: regForm.password })
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = regForm.username
  } catch {} finally { loading.value = false }
}

const sendRegCode = async () => {
  if (!/^1\d{10}$/.test(regForm.phone)) { ElMessage.warning('请先输入有效手机号'); return }
  try {
    const res = await sendCode({ target: regForm.phone, type: 'register' })
    ElMessage.success('验证码已发送（开发环境：' + (res.data || '') + '）')
    codeCountdown.value = 60
    const timer = setInterval(() => { codeCountdown.value--; if (codeCountdown.value <= 0) clearInterval(timer) }, 1000)
  } catch {}
}
</script>

<style scoped>
.login-page { display: flex; height: 100vh; background: var(--color-bg); overflow: hidden; }
.login-banner { flex: 1; display: flex; align-items: center; justify-content: center; position: relative; background: var(--color-surface-soft); border-right: 1px solid var(--color-border-strong); color: var(--color-text); padding: 48px; }
.banner-content { display: flex; flex-direction: column; align-items: center; max-width: 520px; }
.login-brand { margin-bottom: 18px; }
.animation-stage { display: flex; align-items: center; justify-content: center; width: 260px; height: 260px; margin-bottom: 26px; border: 1px solid var(--color-border); border-radius: var(--radius-panel); background: var(--color-surface); box-shadow: var(--shadow-panel); }
.stage-logo { width: 132px; height: 132px; object-fit: contain; }
.banner-content h1 { font-size: 24px; font-weight: 700; margin-bottom: 12px; letter-spacing: 0; }
.banner-content p { font-size: 14px; color: var(--color-text-secondary); text-align: center; line-height: 1.8; margin-bottom: 28px; }
.banner-features { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.feature { font-size: 15px; color: var(--color-text-secondary); background: var(--color-surface); border: 1px solid var(--color-border); padding: 14px 20px; border-radius: 8px; box-shadow: 0 8px 20px rgba(47,125,77,0.06); }
.feature span { margin-right: 8px; }
.login-form-area { width: 480px; display: flex; align-items: center; justify-content: center; background: var(--color-bg); }
.form-card { width: 380px; padding: 28px; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-panel); box-shadow: var(--shadow-panel); }
.login-tabs { margin-bottom: 8px; }
.submit-btn { width: 100%; height: 44px; font-size: 15px; letter-spacing: 0; }
@media (max-width: 900px) {
  .login-page { min-height: 100vh; height: auto; flex-direction: column; }
  .login-banner { flex: none; min-height: 360px; border-right: 0; border-bottom: 1px solid var(--color-border-strong); }
  .animation-stage { width: 200px; height: 200px; margin-bottom: 18px; }
  .stage-logo { width: 104px; height: 104px; }
  .banner-features { grid-template-columns: 1fr 1fr; gap: 12px; }
  .login-form-area { width: 100%; padding: 28px 16px; }
  .form-card { width: min(380px, 100%); }
}
</style>
