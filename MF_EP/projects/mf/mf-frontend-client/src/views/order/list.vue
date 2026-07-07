<template>
  <div class="page-container">
    <h2 class="section-title">我的订单</h2>
    <el-tabs v-model="status" @tab-change="handleStatusChange">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待付款" name="pending_pay" />
      <el-tab-pane label="待发货" name="pending_ship" />
      <el-tab-pane label="已发货" name="shipped" />
      <el-tab-pane label="已完成" name="completed" />
    </el-tabs>
    <div v-if="orders.length === 0 && !loading" style="padding:80px;text-align:center;color:#999">暂无订单</div>
    <div v-for="order in orders" :key="order.id" style="background:#fff;border-radius:8px;padding:16px;margin-bottom:12px;border:1px solid #ebeef5">
      <div style="display:flex;justify-content:space-between;margin-bottom:12px">
        <div>
          <span style="color:#999">订单号：{{ order.orderNo }}</span>
          <div v-if="order.status==='pending_pay'" class="pay-countdown">
            剩余支付时间：{{ getRemainingText(order) }}
          </div>
        </div>
        <el-tag size="small" :type="getOrderStatusType(order.status)">{{ getOrderStatusLabel(order.status) }}</el-tag>
      </div>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>共 {{ order.items?.length || 0 }} 件</span>
        <span style="font-size:18px;color:#e74c3c">{{ formatCurrency(order.payAmount) }}</span>
      </div>
      <div style="margin-top:12px;text-align:right">
        <el-button v-if="order.status==='pending_pay'" type="primary" size="small" @click="goPay(order)">立即支付</el-button>
        <el-button size="small" @click="$router.push('/order/'+order.id)">订单详情</el-button>
      </div>
    </div>
    <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20]" background layout="prev,pager,next" @current-change="fetchList" @size-change="fetchList" style="justify-content:center;margin-top:16px" />
  </div>
</template>
<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList } from '@/api/order'
import { getOrderStatusLabel, getOrderStatusType } from '@/api/orderStatus'
import { formatCurrency } from '@/utils/format'

const router = useRouter()
const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')
const loading = ref(false)
const now = ref(Date.now())
let timer = null

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getOrderList({
      page: page.value,
      size: size.value,
      status: status.value || undefined
    })
    if (res.code === 200 && res.data) {
      orders.value = res.data.records || []
      total.value = Number(res.data.total) || 0
    }
  } catch {
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  page.value = 1
  fetchList()
}

const getRemainingText = (order) => {
  const deadline = getPaymentDeadline(order)
  if (!deadline) return '--:--'
  const remaining = Math.max(0, Math.floor((deadline - now.value) / 1000))
  const minutes = Math.floor(remaining / 60)
  const seconds = remaining % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

const getPaymentDeadline = (order) => {
  if (order.paymentExpireTime) return new Date(order.paymentExpireTime).getTime()
  if (order.createTime) return new Date(order.createTime).getTime() + 60 * 1000
  return 0
}

const goPay = (order) => {
  router.push({ name: 'Cashier', params: { id: order.id } })
}

onMounted(() => {
  fetchList()
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.pay-countdown { margin-top: 4px; color: #e6a23c; font-size: 12px; }
</style>
