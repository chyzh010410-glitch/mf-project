import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/auth/Login.vue'), meta: { title: '商家登录' } },
  { path: '/register', name: 'Register', component: () => import('@/views/auth/Register.vue'), meta: { title: '入驻申请' } },
  {
    path: '/',
    component: () => import('@/views/layout/MerchantLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '首页概览' } },
      { path: 'products', name: 'Products', component: () => import('@/views/products/index.vue'), meta: { title: '商品管理' } },
      { path: 'orders', name: 'Orders', component: () => import('@/views/orders/index.vue'), meta: { title: '订单管理' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/profile/index.vue'), meta: { title: '店铺资料' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('merchantToken')
  document.title = `${to.meta.title || '商家端'} - 苗丰`
  if (!['/login', '/register'].includes(to.path) && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
