import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from './views/Dashboard.vue'
import SourceGovernance from './views/SourceGovernance.vue'
import Products from './views/Products.vue'
import Content from './views/Content.vue'
import Merchants from './views/Merchants.vue'
import AiAnalysis from './views/AiAnalysis.vue'
import UnresolvedQuestions from './views/UnresolvedQuestions.vue'
import SampleCandidates from './views/SampleCandidates.vue'
import MetricGovernance from './views/MetricGovernance.vue'
import Notifications from './views/Notifications.vue'
import DataQuality from './views/DataQuality.vue'
import KnowledgeWorkbench from './views/KnowledgeWorkbench.vue'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: Dashboard, meta: { title: '运营总览' } },
  { path: '/source-governance', component: SourceGovernance, meta: { title: '数据源接入' } },
  { path: '/metric-governance', component: MetricGovernance, meta: { title: '指标治理' } },
  { path: '/data-quality', component: DataQuality, meta: { title: '数据质量' } },
  { path: '/products', component: Products, meta: { title: '商品分析' } },
  { path: '/content', component: Content, meta: { title: '内容分析' } },
  { path: '/merchants', component: Merchants, meta: { title: '商家分析' } },
  { path: '/ai-analysis', component: AiAnalysis, meta: { title: 'AI 咨询分析' } },
  { path: '/unresolved-questions', component: UnresolvedQuestions, meta: { title: '未解决问题池' } },
  { path: '/sample-candidates', component: SampleCandidates, meta: { title: '样本候选池' } }
  ,{ path: '/knowledge-workbench', component: KnowledgeWorkbench, meta: { title: 'AI 知识运营工作台' } }
  ,{ path: '/notifications', component: Notifications, meta: { title: '通知中心' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
