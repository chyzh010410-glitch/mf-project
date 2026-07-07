<template>
  <div class="page-container" v-loading="loading">
    <div class="cashier-page" v-if="order">
      <div class="cashier-main">
        <div class="cashier-header">
          <div>
            <h2>订单支付</h2>
            <p>订单号：{{ order.orderNo }}</p>
          </div>
          <el-tag size="large" :type="getOrderStatusType(order.status)">
            {{ getOrderStatusLabel(order.status) }}
          </el-tag>
        </div>

        <div v-if="order.status === 'pending_pay' && paymentEnabled" class="pay-box">
          <div class="amount-line">
            <span>应付金额</span>
            <strong>{{ formatCurrency(order.payAmount) }}</strong>
          </div>
          <div class="countdown" :class="{ urgent: remainingSeconds <= 30 }">
            {{ remainingSeconds > 0 ? `请在 ${remainingText} 内完成支付，超时订单将自动取消` : '订单已超时，请返回订单列表查看状态' }}
          </div>

          <div class="method-title">选择支付方式</div>
          <el-radio-group v-model="paymentMethod" class="payment-options">
            <el-radio-button label="wechat">微信支付</el-radio-button>
            <el-radio-button label="alipay">支付宝</el-radio-button>
          </el-radio-group>

          <div class="qr-code" :class="paymentMethod">
            <div class="qr-corner top-left"></div>
            <div class="qr-corner top-right"></div>
            <div class="qr-corner bottom-left"></div>
            <span>{{ paymentMethod === 'wechat' ? '微信' : '支付宝' }}</span>
          </div>
          <p class="qr-tip">请使用{{ paymentMethodLabel }}扫码。当前为模拟支付，确认后订单进入待发货。</p>

          <div class="actions">
            <el-button @click="router.push('/orders')">稍后支付</el-button>
            <el-button type="primary" size="large" :loading="paying" :disabled="remainingSeconds <= 0 || !paymentEnabled" @click="finishPay">
              {{ remainingSeconds <= 0 ? '订单已超时' : '我已完成支付' }}
            </el-button>
          </div>
        </div>

        <el-result
          v-else-if="order.status === 'pending_pay' && !paymentEnabled"
          icon="warning"
          title="支付暂未开放"
          sub-title="当前平台已关闭模拟支付，请稍后再试或联系管理员。"
        >
          <template #extra>
            <el-button type="primary" @click="router.push('/orders')">返回订单</el-button>
          </template>
        </el-result>

        <el-result
          v-else-if="order.status === 'pending_ship'"
          icon="success"
          title="订单已支付"
          sub-title="商家将尽快为您发货"
        >
          <template #extra>
            <el-button type="primary" @click="router.push('/orders')">查看订单</el-button>
          </template>
        </el-result>

        <el-result
          v-else
          icon="warning"
          title="当前订单不可支付"
          :sub-title="order.cancelReason || getOrderStatusLabel(order.status)"
        >
          <template #extra>
            <el-button type="primary" @click="router.push('/orders')">返回订单</el-button>
          </template>
        </el-result>
      </div>

      <div class="order-side">
        <h3>商品明细</h3>
        <div v-for="item in order.items || []" :key="item.productId" class="order-item">
          <span>{{ item.productName }}</span>
          <em>x{{ item.quantity }}</em>
        </div>
      </div>
    </div>
    <el-empty v-else-if="!loading" description="订单不存在" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPublicConfig } from '@/api/config'
import { getOrderDetail, payOrder } from '@/api/order'
import { getOrderStatusLabel, getOrderStatusType } from '@/api/orderStatus'
import { formatCurrency } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(false)
const paying = ref(false)
const now = ref(Date.now())
const paymentMethod = ref(route.query.method === 'alipay' ? 'alipay' : 'wechat')
const paymentEnabled = ref(true)
let timer = null

const paymentMethodLabel = computed(() => paymentMethod.value === 'wechat' ? '微信支付' : '支付宝')
const deadline = computed(() => {
  if (order.value?.paymentExpireTime) return new Date(order.value.paymentExpireTime).getTime()
  if (!order.value?.createTime) return 0
  return new Date(order.value.createTime).getTime() + 60 * 1000
})
const remainingSeconds = computed(() => Math.max(0, Math.floor((deadline.value - now.value) / 1000)))
const remainingText = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60)
  const seconds = remainingSeconds.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const fetchOrder = async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id)
    if (res.code === 200) order.value = res.data
  } catch {} finally { loading.value = false }
}

const fetchPaymentConfig = async () => {
  try {
    const res = await getPublicConfig()
    if (res.code === 200 && res.data?.paymentEnabled !== undefined) {
      paymentEnabled.value = Boolean(res.data.paymentEnabled)
    }
  } catch {
    paymentEnabled.value = false
  }
}

const finishPay = async () => {
  if (!order.value) return
  if (!paymentEnabled.value) {
    ElMessage.warning('支付暂未开放')
    return
  }
  if (remainingSeconds.value <= 0) {
    ElMessage.warning('订单已超时，请返回订单列表查看状态')
    await fetchOrder()
    return
  }
  paying.value = true
  try {
    await payOrder(order.value.id, { paymentMethod: paymentMethod.value })
    ElMessage.success('支付成功，订单已进入待发货')
    await fetchOrder()
  } catch {} finally { paying.value = false }
}

onMounted(() => {
  fetchPaymentConfig()
  fetchOrder()
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.cashier-page { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 20px; }
.cashier-main, .order-side { background: #fff; border-radius: 8px; padding: 24px; border: 1px solid #ebeef5; }
.cashier-header { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 24px; }
.cashier-header h2 { margin: 0 0 8px; color: #303133; }
.cashier-header p { margin: 0; color: #909399; }
.pay-box { max-width: 520px; margin: 0 auto; text-align: center; }
.amount-line { display: flex; justify-content: space-between; align-items: center; background: #fff8f0; padding: 16px; border-radius: 8px; margin-bottom: 12px; }
.amount-line strong { color: #e74c3c; font-size: 28px; }
.countdown { color: #2d8c4a; background: #f0f9eb; border-radius: 6px; padding: 8px; margin-bottom: 20px; }
.countdown.urgent { color: #e6a23c; background: #fdf6ec; }
.method-title { text-align: left; font-weight: 600; margin-bottom: 10px; }
.payment-options { width: 100%; margin-bottom: 24px; }
.payment-options :deep(.el-radio-button) { flex: 1; }
.payment-options :deep(.el-radio-button__inner) { width: 100%; }
.qr-code { position: relative; width: 220px; height: 220px; margin: 0 auto 16px; border: 10px solid #fff; box-shadow: 0 0 0 1px #e5e7eb, 0 8px 24px rgba(0,0,0,.08); background-color: #fff; background-image: linear-gradient(90deg, currentColor 12px, transparent 12px), linear-gradient(currentColor 12px, transparent 12px), linear-gradient(45deg, transparent 47%, currentColor 47% 53%, transparent 53%); background-size: 28px 28px, 28px 28px, 36px 36px; color: #2d8c4a; display: flex; align-items: center; justify-content: center; }
.qr-code.alipay { color: #1677ff; }
.qr-code span { background: #fff; color: #303133; border-radius: 50%; width: 58px; height: 58px; display: flex; align-items: center; justify-content: center; font-weight: 700; box-shadow: 0 0 0 8px #fff; }
.qr-corner { position: absolute; width: 46px; height: 46px; border: 8px solid currentColor; background: #fff; }
.qr-corner::after { content: ''; position: absolute; inset: 9px; background: currentColor; }
.qr-corner.top-left { top: 12px; left: 12px; }
.qr-corner.top-right { top: 12px; right: 12px; }
.qr-corner.bottom-left { bottom: 12px; left: 12px; }
.qr-tip { color: #909399; font-size: 13px; line-height: 1.7; margin: 0 0 20px; }
.actions { display: flex; justify-content: center; gap: 12px; }
.order-side h3 { margin: 0 0 16px; }
.order-item { display: flex; justify-content: space-between; gap: 12px; padding: 10px 0; border-bottom: 1px solid #f0f0f0; color: #606266; }
.order-item em { color: #909399; font-style: normal; }
@media (max-width: 900px) {
  .cashier-page { grid-template-columns: 1fr; }
}
</style>
