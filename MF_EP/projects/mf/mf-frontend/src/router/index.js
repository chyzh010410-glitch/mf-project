import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/fertilizer',
    children: [
      { path: 'fertilizer', name: 'Fertilizer', component: () => import('@/views/fertilizer/index.vue'), meta: { title: '肥料管理', icon: 'Shop' } },
      { path: 'tree', name: 'Tree', component: () => import('@/views/tree/index.vue'), meta: { title: '树木管理', icon: 'Grid' } },
      { path: 'record', name: 'Record', component: () => import('@/views/record/index.vue'), meta: { title: '施肥记录', icon: 'Document' } },
      { path: 'rule', name: 'Rule', component: () => import('@/views/rule/index.vue'), meta: { title: '施肥规则', icon: 'Setting' } },
      { path: 'admin/products', name: 'AdminProducts', component: () => import('@/views/admin/Products.vue'), meta: { title: '商品列表' } },
      { path: 'admin/categories', name: 'AdminCategories', component: () => import('@/views/admin/Categories.vue'), meta: { title: '商品分类' } },
      { path: 'admin/uploads', name: 'AdminUploads', component: () => import('@/views/admin/Uploads.vue'), meta: { title: '用户上传审核' } },
      { path: 'admin/encyclopedia', name: 'AdminEncyclopedia', component: () => import('@/views/admin/Encyclopedia.vue'), meta: { title: '百科管理' } },
      { path: 'admin/articles', name: 'AdminArticles', component: () => import('@/views/admin/Articles.vue'), meta: { title: '文章管理' } },
      { path: 'admin/comments', name: 'AdminComments', component: () => import('@/views/admin/Comments.vue'), meta: { title: '评论管理' } },
      { path: 'admin/orders', name: 'AdminOrders', component: () => import('@/views/admin/Orders.vue'), meta: { title: '订单列表' } },
      { path: 'admin/payments', name: 'AdminPayments', component: () => import('@/views/admin/Payments.vue'), meta: { title: '支付流水' } },
      { path: 'admin/users', name: 'AdminUsers', component: () => import('@/views/admin/Users.vue'), meta: { title: '用户列表' } },
      { path: 'admin/merchants', name: 'AdminMerchants', component: () => import('@/views/admin/Merchants.vue'), meta: { title: '商家管理' } },
      { path: 'admin/feedbacks', name: 'AdminFeedbacks', component: () => import('@/views/admin/Feedbacks.vue'), meta: { title: '反馈处理' } },
      { path: 'admin/admins', name: 'AdminAdmins', component: () => import('@/views/admin/Admins.vue'), meta: { title: '管理员管理' } },
      { path: 'admin/config', name: 'AdminConfig', component: () => import('@/views/admin/Config.vue'), meta: { title: '平台设置' } },
      { path: 'admin/faqs', name: 'AdminFaqs', component: () => import('@/views/admin/Faqs.vue'), meta: { title: 'FAQ 管理' } },
      { path: 'admin/activities', name: 'AdminActivities', component: () => import('@/views/admin/Activities.vue'), meta: { title: '活动管理' } },
      { path: 'admin/messages', name: 'AdminMessages', component: () => import('@/views/admin/Messages.vue'), meta: { title: '消息推送' } },
      { path: 'admin/logs', name: 'AdminLogs', component: () => import('@/views/admin/Logs.vue'), meta: { title: '系统日志' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  document.title = `${to.meta.title || '后台管理'} - 苗丰施肥`
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
