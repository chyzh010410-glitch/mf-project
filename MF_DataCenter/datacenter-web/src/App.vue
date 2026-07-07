<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">MF</div>
        <div>
          <strong>DataCenter</strong>
          <span>苗丰数据中台</span>
        </div>
      </div>
      <nav aria-label="主导航">
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path">
          <component :is="item.icon" :size="17" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="main-area">
      <header class="topbar">
        <div>
          <p>企业级数据资产与治理控制台</p>
          <h1>{{ route.meta.title }}</h1>
        </div>
        <el-tag effect="plain" type="success">MySQL · 快照 · 治理</el-tag>
      </header>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { Bot, CircleHelp, ClipboardCheck, DatabaseZap, FileCheck2, LayoutDashboard, Newspaper, Package, PlugZap, Store } from 'lucide-vue-next'

const route = useRoute()
const navItems = [
  { path: '/dashboard', label: '运营总览', icon: LayoutDashboard },
  { path: '/source-governance', label: '数据源接入', icon: PlugZap },
  { path: '/metric-governance', label: '指标治理', icon: DatabaseZap },
  { path: '/data-quality', label: '数据质量', icon: ClipboardCheck },
  { path: '/products', label: '商品分析', icon: Package },
  { path: '/content', label: '内容分析', icon: Newspaper },
  { path: '/merchants', label: '商家分析', icon: Store },
  { path: '/ai-analysis', label: 'AI 咨询分析', icon: Bot },
  { path: '/unresolved-questions', label: '问题池', icon: CircleHelp },
  { path: '/sample-candidates', label: '样本池', icon: FileCheck2 }
]
</script>
