<template>
  <div class="page-container">
    <div class="toolbar">
      <span class="toolbar-title">支付流水</span>
    </div>

    <el-form :inline="true" :model="query" class="filter-form">
      <el-form-item label="订单号">
        <el-input v-model="query.orderNo" clearable placeholder="输入订单号" />
      </el-form-item>
      <el-form-item label="交易号">
        <el-input v-model="query.tradeNo" clearable placeholder="输入交易号" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
          <el-option label="支付成功" value="success" />
          <el-option label="待支付" value="pending" />
          <el-option label="已退款" value="refunded" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
      <el-table-column prop="tradeNo" label="交易号" min-width="210" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户ID" width="90" align="center" />
      <el-table-column prop="payMethod" label="支付方式" width="110" align="center">
        <template #default="{ row }">{{ getPaymentMethodLabel(row.payMethod) }}</template>
      </el-table-column>
      <el-table-column prop="amount" label="支付金额" width="110" align="right">
        <template #default="{ row }">{{ formatCurrency(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getPaymentStatusType(row.status)" size="small">{{ getPaymentStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="payTime" label="支付时间" width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.payTime || '-' }}</template>
      </el-table-column>
      <el-table-column prop="refundAmount" label="退款金额" width="110" align="right">
        <template #default="{ row }">{{ row.refundAmount ? formatCurrency(row.refundAmount) : '-' }}</template>
      </el-table-column>
      <el-table-column prop="refundTime" label="退款时间" width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.refundTime || '-' }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total,sizes,prev,pager,next,jumper"
      background
      style="justify-content:flex-end;margin-top:16px"
      @current-change="fetchData"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getPaymentPage } from '@/api/admin'
import { getPaymentMethodLabel } from '@/api/orderStatus'
import { formatCurrency } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const query = reactive({
  orderNo: '',
  tradeNo: '',
  status: ''
})

const paymentStatusMap = {
  success: '支付成功',
  pending: '待支付',
  refunded: '已退款'
}

const paymentStatusType = {
  success: 'success',
  pending: 'warning',
  refunded: 'info'
}

const getPaymentStatusLabel = (status) => paymentStatusMap[status] || status || '-'
const getPaymentStatusType = (status) => paymentStatusType[status] || ''

const buildParams = () => {
  const params = { page: page.value, size: size.value }
  if (query.orderNo) params.orderNo = query.orderNo
  if (query.tradeNo) params.tradeNo = query.tradeNo
  if (query.status) params.status = query.status
  return params
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPaymentPage(buildParams())
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {} finally { loading.value = false }
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const handleReset = () => {
  query.orderNo = ''
  query.tradeNo = ''
  query.status = ''
  handleSearch()
}

const handleSizeChange = () => {
  page.value = 1
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}
</style>
