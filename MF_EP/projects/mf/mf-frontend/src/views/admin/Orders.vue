<template>
  <div class="page-container">
    <div class="toolbar"><span class="toolbar-title">订单列表</span></div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="4" v-for="s in stats" :key="s.label">
        <div class="stat-card"><div class="stat-num">{{ s.count }}</div><div class="stat-label">{{ s.label }}</div></div>
      </el-col>
    </el-row>

    <!-- 状态筛选 -->
    <el-tabs v-model="activeStatus" @tab-change="handleStatusChange">
      <el-tab-pane v-for="item in orderStatusOptions" :key="item.value" :label="item.label" :name="item.value" />
    </el-tabs>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="订单编号" width="200" />
      <el-table-column prop="userId" label="用户ID" width="100" align="center" />
      <el-table-column prop="totalAmount" label="金额" width="100" align="center">
        <template #default="{ row }">{{ formatCurrency(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="payAmount" label="实付" width="100" align="center">
        <template #default="{ row }">{{ formatCurrency(row.payAmount) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getOrderStatusType(row.status)" size="small">{{ getOrderStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="paymentMethod" label="支付方式" width="100" align="center">
        <template #default="{ row }">{{ getPaymentMethodLabel(row.paymentMethod) }}</template>
      </el-table-column>
      <el-table-column prop="payTime" label="支付时间" width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.payTime || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending_pay'" type="success" size="small" @click="updateStatus(row, 'pending_ship')">标记已付款</el-button>
          <el-button v-if="row.status === 'pending_ship'" type="primary" size="small" @click="openShip(row)">发货</el-button>
          <el-button v-if="row.status === 'pending_pay'" type="warning" size="small" @click="updateStatus(row, 'cancelled')" style="margin-left:4px">取消</el-button>
          <el-button v-if="canRefund(row)" type="danger" size="small" @click="handleRefund(row)" style="margin-left:4px">退款</el-button>
          <el-button size="small" style="margin-left:4px" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page" v-model:page-size="size"
      :total="total" :page-sizes="[10,20,50]"
      layout="total,sizes,prev,pager,next,jumper"
      background style="justify-content:flex-end;margin-top:16px"
      @current-change="fetchData" @size-change="fetchData"
    />

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipVisible" title="发货" width="420px" destroy-on-close>
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="80px">
        <el-form-item label="物流公司" prop="logisticsCompany">
          <el-select v-model="shipForm.logisticsCompany" style="width:100%" placeholder="选择物流">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="京东物流" value="京东物流" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号" prop="logisticsNo">
          <el-input v-model="shipForm.logisticsNo" placeholder="输入运单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipping" @click="doShip">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="640px">
      <template v-if="detailOrder">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单编号">{{ detailOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getOrderStatusType(detailOrder.status)" size="small">{{ getOrderStatusLabel(detailOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="商品总额">{{ formatCurrency(detailOrder.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">{{ formatCurrency(detailOrder.payAmount) }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ getPaymentMethodLabel(detailOrder.paymentMethod) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ detailOrder.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注/售后" :span="2" v-if="detailOrder.adminRemark">{{ detailOrder.adminRemark }}</el-descriptions-item>
          <el-descriptions-item label="下单时间" :span="2">{{ detailOrder.createTime }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin-top:16px">商品明细</h4>
        <el-table :data="detailItems" size="small" border>
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="price" label="单价" width="90" />
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column prop="totalPrice" label="小计" width="100" />
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, getOrderPage, getOrderStatistics, refundOrder, shipOrder, updateOrderStatus } from '@/api/admin'
import {
  buildOrderStats,
  getOrderStatusLabel,
  getOrderStatusType,
  getPaymentMethodLabel,
  orderStatusActions,
  orderStatusOptions
} from '@/api/orderStatus'
import { formatCurrency } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const activeStatus = ref('')

const stats = ref(buildOrderStats())

// Ship dialog
const shipVisible = ref(false)
const shipping = ref(false)
const shipForm = reactive({ logisticsCompany: '', logisticsNo: '' })
const shipFormRef = ref(null)
const shipTargetId = ref(null)
const shipRules = {
  logisticsCompany: [{ required: true, message: '请选择物流公司', trigger: 'change' }],
  logisticsNo: [{ required: true, message: '请输入物流单号', trigger: 'blur' }]
}

// Detail dialog
const detailVisible = ref(false)
const detailOrder = ref(null)
const detailItems = ref([])

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (activeStatus.value) params.status = activeStatus.value
    const res = await getOrderPage(params)
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
    // Fetch stats
    const statRes = await getOrderStatistics()
    if (statRes.code === 200 && statRes.data) {
      stats.value = buildOrderStats(statRes.data)
    }
  } catch {} finally { loading.value = false }
}

const openShip = (row) => {
  shipTargetId.value = row.id
  shipForm.logisticsCompany = ''
  shipForm.logisticsNo = ''
  shipVisible.value = true
}

const doShip = async () => {
  const valid = await shipFormRef.value.validate().catch(() => false)
  if (!valid) return
  shipping.value = true
  try {
    const res = await shipOrder(shipTargetId.value, shipForm)
    if (res.code === 200) {
      ElMessage.success('发货成功')
      shipVisible.value = false
      fetchData()
    }
  } catch {} finally { shipping.value = false }
}

const updateStatus = async (row, status) => {
  try {
    const res = await updateOrderStatus(row.id, status)
    if (res.code === 200) {
      ElMessage.success(orderStatusActions[status] || '状态已更新')
      fetchData()
    }
  } catch {}
}

const canRefund = (row) => ['pending_ship', 'shipped', 'completed', 'refund_requested'].includes(row.status)

const handleRefund = async (row) => {
  try {
    await ElMessageBox.confirm(`确认对订单 ${row.orderNo} 执行模拟退款？`, '退款确认', {
      type: 'warning',
      confirmButtonText: '确认退款',
      cancelButtonText: '取消'
    })
    const res = await refundOrder(row.id, { reason: '管理员模拟退款' })
    if (res.code === 200) {
      ElMessage.success('退款成功')
      fetchData()
    }
  } catch {}
}

const handleStatusChange = () => {
  page.value = 1
  fetchData()
}

const openDetail = async (row) => {
  detailVisible.value = true
  try {
    const res = await getOrderDetail(row.id)
    if (res.code === 200 && res.data) {
      detailOrder.value = res.data.order
      detailItems.value = res.data.items || []
    }
  } catch {}
}

onMounted(fetchData)
</script>

<style scoped>
.stat-card {
  min-height: 88px;
  padding: 14px;
  text-align: left;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-panel);
  background: var(--color-surface-soft);
}

.stat-num {
  margin-bottom: 8px;
  color: var(--color-text);
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}

.stat-label {
  color: var(--color-text-secondary);
  font-size: 13px;
}
</style>
