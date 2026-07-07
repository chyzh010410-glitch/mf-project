import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/login/index.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('@/views/layout/DefaultLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/home/index.vue'), meta: { title: '首页' } },
      { path: 'products', name: 'ProductList', component: () => import('@/views/product/list.vue'), meta: { title: '商品列表' } },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('@/views/product/detail.vue'), meta: { title: '商品详情' } },
      { path: 'cart', name: 'Cart', component: () => import('@/views/cart/index.vue'), meta: { title: '购物车' } },
      { path: 'checkout', name: 'Checkout', component: () => import('@/views/checkout/index.vue'), meta: { title: '确认订单' } },
      { path: 'pay/:id', name: 'Cashier', component: () => import('@/views/payment/cashier.vue'), meta: { title: '订单支付' } },
      { path: 'orders', name: 'OrderList', component: () => import('@/views/order/list.vue'), meta: { title: '我的订单' } },
      { path: 'order/:id', name: 'OrderDetail', component: () => import('@/views/order/detail.vue'), meta: { title: '订单详情' } },
      { path: 'user/profile', name: 'Profile', component: () => import('@/views/user/profile.vue'), meta: { title: '个人中心' } },
      { path: 'user/addresses', name: 'Addresses', component: () => import('@/views/user/address.vue'), meta: { title: '收货地址' } },
      { path: 'user/security', name: 'Security', component: () => import('@/views/user/security.vue'), meta: { title: '账户安全' } },
      { path: 'user/points', name: 'Points', component: () => import('@/views/user/points.vue'), meta: { title: '我的积分' } },
      { path: 'encyclopedia', name: 'Encyclopedia', component: () => import('@/views/encyclopedia/list.vue'), meta: { title: '树木百科' } },
      { path: 'encyclopedia/:id', name: 'EncyclopediaDetail', component: () => import('@/views/encyclopedia/detail.vue'), meta: { title: '百科详情' } },
      { path: 'articles', name: 'ArticleList', component: () => import('@/views/article/list.vue'), meta: { title: '科普文章' } },
      { path: 'article/:id', name: 'ArticleDetail', component: () => import('@/views/article/detail.vue'), meta: { title: '文章详情' } },
      { path: 'favorites', name: 'Favorites', component: () => import('@/views/favorite/index.vue'), meta: { title: '我的收藏' } },
      { path: 'history', name: 'History', component: () => import('@/views/history/index.vue'), meta: { title: '浏览历史' } },
      { path: 'messages', name: 'Messages', component: () => import('@/views/message/index.vue'), meta: { title: '消息中心' } },
      { path: 'faq', name: 'Faq', component: () => import('@/views/faq/index.vue'), meta: { title: '帮助中心' } },
      { path: 'feedback', name: 'Feedback', component: () => import('@/views/feedback/index.vue'), meta: { title: '意见反馈' } },
      { path: 'activities', name: 'Activities', component: () => import('@/views/activity/index.vue'), meta: { title: '优惠活动' } },
      { path: 'ai', name: 'AiChat', component: () => import('@/views/ai/index.vue'), meta: { title: 'AI 智能客服' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('clientToken')
  document.title = `${to.meta.title || '首页'} - 苗丰施肥`
  if (to.path !== '/login' && !token) next('/login')
  else if (to.path === '/login' && token) next('/')
  else next()
})

export default router
