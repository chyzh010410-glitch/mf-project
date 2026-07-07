<template>
  <div class="layout">
    <el-aside width="220px" class="layout-sidebar">
      <div class="sidebar-logo brand-lockup">
        <span class="brand-logo-box"><img :src="logoUrl" alt="MF" class="brand-logo" /></span>
      </div>
      <div class="sidebar-header">苗丰商家端</div>
      <el-menu router :default-active="activeMenu" background-color="#173a22" text-color="#cfe0d2" active-text-color="#fff">
        <el-menu-item index="/dashboard"><el-icon><DataBoard /></el-icon><span>首页概览</span></el-menu-item>
        <el-menu-item index="/products"><el-icon><Goods /></el-icon><span>商品管理</span></el-menu-item>
        <el-menu-item index="/orders"><el-icon><Tickets /></el-icon><span>订单管理</span></el-menu-item>
        <el-menu-item index="/profile"><el-icon><Shop /></el-icon><span>店铺资料</span></el-menu-item>
      </el-menu>
    </el-aside>
    <div class="layout-main">
      <header class="layout-header">
        <span class="header-title">{{ pageTitle }}</span>
        <div class="header-right">
          <span>{{ merchantInfo?.shopName || merchantInfo?.username || '商家' }}</span>
          <el-button type="danger" text size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </header>
      <main class="layout-content"><router-view /></main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataBoard, Goods, Tickets, Shop } from '@element-plus/icons-vue'
import { logoutMerchant } from '@/api/merchant'
import logoUrl from '@/assets/brand/logo.png'

const route = useRoute()
const router = useRouter()
const merchantInfo = computed(() => JSON.parse(localStorage.getItem('merchantInfo') || 'null'))
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '')

const handleLogout = async () => {
  try {
    await logoutMerchant()
  } catch {
    // Keep local logout reliable even if the token is already expired.
  }
  localStorage.removeItem('merchantToken')
  localStorage.removeItem('merchantInfo')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  background: var(--color-bg);
}

.layout-sidebar {
  width: var(--sidebar-width);
  background: #173a22;
  box-shadow: 10px 0 30px rgba(23, 58, 34, 0.12);
}

.sidebar-logo {
  justify-content: center;
  height: 54px;
  padding-top: 14px;
}

.sidebar-header {
  height: 46px;
  line-height: 34px;
  padding: 0 16px 12px;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  text-align: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.layout-sidebar .el-menu {
  border-right: none;
}

.layout-sidebar .el-menu-item.is-active {
  background: var(--color-primary) !important;
}

.layout-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--header-height);
  padding: 0 24px;
  background: rgba(255,255,255,0.92);
  border-bottom: 1px solid var(--color-border);
  box-shadow: 0 10px 28px rgba(47,125,77,0.08);
  backdrop-filter: blur(12px);
}

.header-title {
  color: var(--color-primary-dark);
  font-weight: 700;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
  color: var(--color-text-secondary);
}

.layout-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
</style>
