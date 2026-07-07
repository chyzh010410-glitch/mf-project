<template>
  <div class="page-container" v-loading="loading">
    <div class="back-bar">
      <el-button text :icon="ArrowLeft" @click="$router.push('/orders')">返回订单列表</el-button>
    </div>
    <div v-if="order">
      <div class="status-header">
        <h2 class="section-title" style="margin-bottom:8px">订单详情</h2>
        <el-tag size="large" :type="getOrderStatusType(order.status)">{{ getOrderStatusLabel(order.status) }}</el-tag>
      </div>
      <el-card style="margin-bottom:16px">
        <template #header><span>订单信息</span></template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ order.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商品总额">{{ formatCurrency(order.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="运费">{{ formatCurrency(order.freightAmount) }}</el-descriptions-item>
          <el-descriptions-item label="优惠金额">{{ formatCurrency(order.discountAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额"><span style="font-size:16px;color:#e74c3c;font-weight:600">{{ formatCurrency(order.payAmount) }}</span></el-descriptions-item>
          <el-descriptions-item label="待支付倒计时" v-if="order.status==='pending_pay'">{{ remainingText }}</el-descriptions-item>
          <el-descriptions-item label="支付方式" v-if="order.paymentMethod">{{ getPaymentMethodLabel(order.paymentMethod) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间" v-if="order.payTime">{{ order.payTime }}</el-descriptions-item>
          <el-descriptions-item label="物流公司" v-if="showLogisticsInfo">{{ order.logisticsCompany || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物流单号" v-if="showLogisticsInfo">{{ order.logisticsNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物流信息" :span="2" v-else-if="showLogisticsRemark">{{ order.adminRemark }}</el-descriptions-item>
          <el-descriptions-item label="退款申请" :span="2" v-if="showRefundRequestInfo">{{ order.adminRemark || '退款申请已提交，等待商家处理' }}</el-descriptions-item>
          <el-descriptions-item label="退款说明" :span="2" v-if="showRefundInfo">{{ order.adminRemark || '订单已退款' }}</el-descriptions-item>
          <el-descriptions-item label="取消原因" v-if="order.cancelReason">{{ order.cancelReason }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2" v-if="order.addressSnapshot">{{ order.addressSnapshot }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
      <el-card style="margin-bottom:16px">
        <template #header><span>商品明细</span></template>
        <el-table :data="order.items||[]" border stripe size="small">
          <el-table-column label="商品" min-width="160">
            <template #default="{row}">{{ row.productName }}</template>
          </el-table-column>
          <el-table-column prop="price" label="单价" width="100" align="center">
            <template #default="{row}">{{ formatCurrency(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="小计" width="100" align="center">
            <template #default="{row}">{{ formatCurrency(row.totalPrice) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
      <div style="text-align:right">
        <el-button v-if="order.status==='pending_pay'" type="primary" @click="goPay">立即支付</el-button>
        <el-button v-if="order.status==='pending_pay'" type="danger" @click="handleCancel">取消订单</el-button>
        <el-button v-if="canRequestRefund" type="warning" @click="handleRefundRequest">申请退款</el-button>
        <el-button v-if="order.status==='shipped'" type="primary" @click="handleConfirm">确认收货</el-button>
      </div>
    </div>
    <el-empty v-if="!loading && !order" description="订单不存在" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getOrderDetail, cancelOrder, confirmOrder, requestRefund } from '@/api/order'
import { getOrderStatusLabel, getOrderStatusType, getPaymentMethodLabel } from '@/api/orderStatus'
import { formatCurrency } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(false)
const now = ref(Date.now())
let timer = null

const remainingText = computed(() => {
  const deadline = getPaymentDeadline()
  if (!deadline) return '--:--'
  const remaining = Math.max(0, Math.floor((deadline - now.value) / 1000))
  const minutes = Math.floor(remaining / 60)
  const seconds = remaining % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const showLogisticsInfo = computed(() => {
  return ['shipped', 'completed'].includes(order.value?.status)
    && Boolean(order.value?.logisticsCompany || order.value?.logisticsNo)
})

const showLogisticsRemark = computed(() => {
  return ['shipped', 'completed'].includes(order.value?.status) && Boolean(order.value?.adminRemark)
})

const showRefundInfo = computed(() => order.value?.status === 'refunded')

const showRefundRequestInfo = computed(() => order.value?.status === 'refund_requested')

const canRequestRefund = computed(() => ['pending_ship', 'shipped', 'completed'].includes(order.value?.status))

const getPaymentDeadline = () => {
  if (order.value?.paymentExpireTime) return new Date(order.value.paymentExpireTime).getTime()
  if (order.value?.createTime) return new Date(order.value.createTime).getTime() + 60 * 1000
  return 0
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id)
    if (res.code === 200) order.value = res.data
  } catch {} finally { loading.value = false }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.prompt('请输入取消原因', '取消订单', { confirmButtonText:'确认取消', type:'warning' }).then(async ({value}) => {
      await cancelOrder(order.value.id, { reason: value })
      ElMessage.success('订单已取消')
      fetchDetail()
    }).catch(() => {})
  } catch {}
}

const goPay = () => {
  router.push({ name: 'Cashier', params: { id: order.value.id } })
}

const handleConfirm = async () => {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '确认收货', { type:'success' })
    await confirmOrder(order.value.id)
    ElMessage.success('已确认收货')
    fetchDetail()
  } catch {}
}

const handleRefundRequest = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入退款原因', '申请退款', {
      confirmButtonText: '提交申请',
      cancelButtonText: '取消',
      type: 'warning',
      inputValidator: value => Boolean(value?.trim()) || '请输入退款原因'
    })
    await requestRefund(order.value.id, { reason: value })
    ElMessage.success('退款申请已提交')
    fetchDetail()
  } catch {}
}

onMounted(() => {
  fetchDetail()
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.back-bar { margin-bottom: 12px; }
.status-header { display: flex; align-items: center; gap: 12px; }
</style>
