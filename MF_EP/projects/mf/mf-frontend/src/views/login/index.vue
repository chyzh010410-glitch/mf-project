<template>
  <div class="login-page">
    <!-- 左侧动画区 -->
    <div class="login-animation">
      <div class="animation-stage">
        <!-- 水滴 -->
        <div class="water-drop">
          <div class="drop-body"></div>
        </div>
        <!-- 涟漪 -->
        <div class="ripple ripple-1"></div>
        <div class="ripple ripple-2"></div>
        <!-- 种子/地面 -->
        <div class="seed"></div>
        <!-- 根系 -->
        <div class="roots">
          <div class="root root-left"></div>
          <div class="root root-right"></div>
          <div class="root root-center"></div>
        </div>
        <!-- 茎干 -->
        <div class="stem"></div>
        <!-- 叶片 -->
        <div class="leaf leaf-left"></div>
        <div class="leaf leaf-right"></div>
      </div>
      <div class="animation-label">苗丰施肥管控平台</div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-form-area">
      <div class="login-card">
        <h2 class="login-title">欢迎登录</h2>
        <p class="login-subtitle">苗丰施肥管控平台后台管理系统</p>
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          size="large"
          class="login-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="formData.username"
              prefix-icon="User"
              placeholder="请输入用户名"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              prefix-icon="Lock"
              placeholder="请输入密码"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { login } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const formData = reactive({
  username: '',
  password: ''
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({
      username: formData.username,
      password: formData.password
    })
    if (res.code === 200 && res.data) {
      authStore.setToken(res.data.token)
      authStore.setUserInfo({
        username: res.data.username,
        realName: res.data.realName,
        role: res.data.role
      })
      ElMessage.success('登录成功')
      router.push('/')
    }
  } catch {
    // 错误已在 request 拦截器中统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  background: var(--color-bg);
  overflow: hidden;
}

/* ===== 左侧动画区 ===== */
.login-animation {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  background: var(--color-surface-soft);
  border-right: 1px solid var(--color-border-strong);
}

.animation-stage {
  position: relative;
  width: 260px;
  height: 360px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-panel);
  background: var(--color-surface);
  box-shadow: var(--shadow-panel);
}

.animation-label {
  margin-top: 26px;
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: 0;
}

/* 水滴 */
.water-drop {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  animation: dropFall 1.6s cubic-bezier(0.33, 0.66, 0.66, 1) forwards;
  z-index: 10;
}

.drop-body {
  width: 18px;
  height: 26px;
  background: linear-gradient(180deg, #9ccf8d 0%, var(--color-primary) 100%);
  border-radius: 50% 0 50% 50%;
  transform: rotate(45deg);
  opacity: 0.9;
  box-shadow: inset -2px -2px 4px rgba(0,0,0,0.08), 0 2px 8px rgba(47,125,77,0.24);
}

@keyframes dropFall {
  0% {
    top: -40px;
    opacity: 0;
  }
  15% {
    opacity: 0.9;
  }
  85% {
    top: 260px;
    opacity: 0.9;
  }
  100% {
    top: 280px;
    opacity: 0;
  }
}

/* 涟漪 */
.ripple {
  position: absolute;
  top: 280px;
  left: 50%;
  transform: translate(-50%, -50%);
  border: 2px solid rgba(47,125,77,0.32);
  border-radius: 50%;
  opacity: 0;
}

.ripple-1 {
  animation: rippleOut 1s 1.45s ease-out forwards;
}

.ripple-2 {
  animation: rippleOut 1s 1.6s ease-out forwards;
}

@keyframes rippleOut {
  0% {
    width: 0;
    height: 0;
    opacity: 0.7;
  }
  100% {
    width: 100px;
    height: 100px;
    opacity: 0;
  }
}

/* 种子 */
.seed {
  position: absolute;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  width: 12px;
  height: 18px;
  background: radial-gradient(ellipse, #5a3e2b 0%, #3d2517 100%);
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
  opacity: 0;
  animation: seedAppear 0.6s 1.8s ease-out forwards;
}

@keyframes seedAppear {
  0% {
    opacity: 0;
    transform: translateX(-50%) scale(0.3);
  }
  100% {
    opacity: 1;
    transform: translateX(-50%) scale(1);
  }
}

/* 根系 */
.roots {
  position: absolute;
  bottom: 60px;
  left: 50%;
  transform: translateX(-50%);
}

.root {
  position: absolute;
  bottom: 0;
  background: #8b7355;
  border-radius: 1px;
  opacity: 0;
  transform-origin: top center;
}

.root-left {
  left: -14px;
  width: 2px;
  height: 0;
  animation: rootGrow 0.8s 2.2s ease-out forwards;
  transform: rotate(25deg);
}

.root-right {
  left: 12px;
  width: 2px;
  height: 0;
  animation: rootGrow 0.8s 2.3s ease-out forwards;
  transform: rotate(-25deg);
}

.root-center {
  left: -1px;
  width: 2px;
  height: 0;
  animation: rootGrow 0.8s 2.4s ease-out forwards;
}

@keyframes rootGrow {
  0% {
    height: 0;
    opacity: 0;
  }
  100% {
    height: 30px;
    opacity: 0.7;
  }
}

/* 茎干 */
.stem {
  position: absolute;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  width: 3px;
  height: 0;
  background: linear-gradient(180deg, var(--color-primary-light) 0%, var(--color-primary) 100%);
  border-radius: 2px;
  opacity: 0;
  transform-origin: bottom center;
  animation: stemGrow 1.2s 2.6s ease-out forwards;
}

@keyframes stemGrow {
  0% {
    height: 0;
    opacity: 0;
  }
  100% {
    height: 100px;
    opacity: 1;
  }
}

/* 叶片 */
.leaf {
  position: absolute;
  bottom: 180px;
  width: 0;
  height: 0;
  opacity: 0;
}

.leaf-left {
  left: calc(50% - 3px);
  background: linear-gradient(135deg, #6bab72 0%, var(--color-primary) 100%);
  border-radius: 0 80% 0 80%;
  transform-origin: bottom right;
  animation: leafUnfurl 0.8s 3.2s ease-out forwards;
}

.leaf-right {
  left: calc(50% - 3px);
  background: linear-gradient(225deg, #6bab72 0%, var(--color-primary) 100%);
  border-radius: 80% 0 80% 0;
  transform-origin: bottom left;
  animation: leafUnfurlRight 0.8s 3.5s ease-out forwards;
}

@keyframes leafUnfurl {
  0% {
    width: 0;
    height: 0;
    opacity: 0;
    transform: rotate(0deg);
  }
  100% {
    width: 40px;
    height: 24px;
    opacity: 1;
    transform: rotate(-30deg) translate(-5px, -5px);
  }
}

@keyframes leafUnfurlRight {
  0% {
    width: 0;
    height: 0;
    opacity: 0;
    transform: rotate(0deg);
  }
  100% {
    width: 40px;
    height: 24px;
    opacity: 1;
    transform: rotate(30deg) translate(5px, -5px);
  }
}

/* ===== 右侧表单区 ===== */
.login-form-area {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}

.login-card {
  width: 360px;
  padding: 28px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-panel);
  background: var(--color-surface);
  box-shadow: var(--shadow-panel);
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
  letter-spacing: 0;
}

.login-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 28px;
}

.login-form .el-form-item {
  margin-bottom: 22px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 0;
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}

.login-btn:hover {
  background-color: var(--color-primary-light);
  border-color: var(--color-primary-light);
}

@media (max-width: 900px) {
  .login-page {
    min-height: 100vh;
    height: auto;
    flex-direction: column;
  }

  .login-animation {
    flex: none;
    min-height: 360px;
    border-right: 0;
    border-bottom: 1px solid var(--color-border-strong);
  }

  .animation-stage {
    transform: scale(0.82);
  }

  .animation-label {
    margin-top: 0;
  }

  .login-form-area {
    width: 100%;
    padding: 28px 16px;
  }

  .login-card {
    width: min(360px, 100%);
  }
}
</style>
