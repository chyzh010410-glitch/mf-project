<template>
  <div class="layout">
    <el-aside :width="isCollapse ? '72px' : '236px'" class="layout-sidebar">
      <div class="sidebar-header">
        <div class="sidebar-brand-mark">MF</div>
        <div v-if="!isCollapse" class="sidebar-brand-copy">
          <strong>MF_EP</strong>
          <span>苗丰运营后台</span>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="#f7fbf6"
        text-color="#344f3b"
        active-text-color="#173a22"
      >
        <el-menu-item index="/fertilizer"><el-icon><Shop /></el-icon><span>肥料管理</span></el-menu-item>
        <el-menu-item index="/tree"><el-icon><Grid /></el-icon><span>树木管理</span></el-menu-item>
        <el-menu-item index="/record"><el-icon><Document /></el-icon><span>施肥记录</span></el-menu-item>
        <el-menu-item index="/rule"><el-icon><Setting /></el-icon><span>施肥规则</span></el-menu-item>
        <el-sub-menu index="mall-group">
          <template #title><el-icon><Goods /></el-icon><span>商城管理</span></template>
          <el-menu-item index="/admin/products"><span>商品列表</span></el-menu-item>
          <el-menu-item index="/admin/categories"><span>商品分类</span></el-menu-item>
          <el-menu-item index="/admin/uploads"><span>用户上传审核</span></el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="content-group">
          <template #title><el-icon><Collection /></el-icon><span>内容管理</span></template>
          <el-menu-item index="/admin/encyclopedia"><span>百科管理</span></el-menu-item>
          <el-menu-item index="/admin/articles"><span>文章管理</span></el-menu-item>
          <el-menu-item index="/admin/comments"><span>评论管理</span></el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="order-group">
          <template #title><el-icon><Tickets /></el-icon><span>订单管理</span></template>
          <el-menu-item index="/admin/orders"><span>订单列表</span></el-menu-item>
          <el-menu-item index="/admin/payments"><span>支付流水</span></el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="user-group">
          <template #title><el-icon><UserFilled /></el-icon><span>用户管理</span></template>
          <el-menu-item index="/admin/users"><span>用户列表</span></el-menu-item>
          <el-menu-item index="/admin/merchants"><span>商家管理</span></el-menu-item>
          <el-menu-item index="/admin/feedbacks"><span>反馈处理</span></el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="system-group">
          <template #title><el-icon><Tools /></el-icon><span>系统管理</span></template>
          <el-menu-item index="/admin/admins"><span>管理员管理</span></el-menu-item>
          <el-menu-item index="/admin/config"><span>平台设置</span></el-menu-item>
          <el-menu-item index="/admin/faqs"><span>FAQ 管理</span></el-menu-item>
          <el-menu-item index="/admin/activities"><span>活动管理</span></el-menu-item>
          <el-menu-item index="/admin/messages"><span>消息推送</span></el-menu-item>
          <el-menu-item index="/admin/logs"><span>系统日志</span></el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <div class="layout-main">
      <header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-icon" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <div>
            <p class="header-kicker">苗丰电商平台运营控制台</p>
            <span class="header-breadcrumb">{{ pageTitle }}</span>
          </div>
        </div>
        <div class="header-right">
          <el-tag effect="plain" type="success">MF_EP · Admin</el-tag>
          <span class="header-user">{{ authStore.userInfo?.realName || authStore.userInfo?.username || '管理员' }}</span>
          <el-button type="danger" text size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </header>

      <main class="layout-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { logout } from '@/api/auth'
import { Shop, Grid, Document, Setting, Goods, Collection, Tickets, UserFilled, Tools, Fold, Expand } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapse = ref(false)

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '')

const handleLogout = async () => {
  try {
    await logout()
  } catch {
    // Ignore logout API failures and clear local session.
  }
  authStore.logout()
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
  background-color: var(--color-surface-soft);
  border-right: 1px solid var(--color-border-strong);
  overflow-y: auto;
  transition: width 0.3s;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  height: var(--header-height);
  padding: 0 14px;
  color: var(--color-text);
}

.sidebar-brand-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  flex: 0 0 38px;
  border-radius: 8px;
  background: var(--color-primary);
  color: #f6fff7;
  font-weight: 800;
}

.sidebar-brand-copy {
  min-width: 0;
}

.sidebar-brand-copy strong,
.sidebar-brand-copy span {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-brand-copy strong {
  font-size: 16px;
  line-height: 1.2;
}

.sidebar-brand-copy span {
  margin-top: 3px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.layout-sidebar .el-menu {
  border-right: none;
  padding: 0 10px 18px;
}

.layout-sidebar .el-menu-item {
  height: 38px;
  margin: 2px 0;
  border-radius: 8px;
  font-size: 14px;
}

.layout-sidebar :deep(.el-sub-menu__title) {
  height: 38px;
  margin: 2px 0;
  border-radius: 8px;
  color: #344f3b;
}

.layout-sidebar .el-menu-item.is-active {
  color: var(--color-primary-dark) !important;
  background-color: var(--color-primary-bg) !important;
  font-weight: 700;
}

.layout-sidebar .el-menu-item:hover,
.layout-sidebar :deep(.el-sub-menu__title:hover) {
  color: var(--color-primary-dark) !important;
  background-color: var(--color-primary-bg) !important;
}

.layout-sidebar .el-menu-item.is-active:hover {
  background-color: var(--color-primary-bg) !important;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--header-height);
  padding: 0 24px;
  background: var(--color-bg);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-icon {
  font-size: 20px;
  cursor: pointer;
  color: var(--color-text-secondary);
  padding: 5px;
  border-radius: 8px;
}

.collapse-icon:hover {
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

.header-kicker {
  margin: 0 0 4px;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1;
}

.header-breadcrumb {
  display: block;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--color-text);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-user {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.layout-content {
  flex: 1;
  padding: 0 24px 24px;
  overflow-y: auto;
  background-color: var(--color-bg);
}

@media (max-width: 760px) {
  .layout-header {
    height: auto;
    min-height: var(--header-height);
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
    padding: 16px;
  }

  .header-right {
    flex-wrap: wrap;
    gap: 10px;
  }

  .layout-content {
    padding: 0 16px 16px;
  }
}
</style>
