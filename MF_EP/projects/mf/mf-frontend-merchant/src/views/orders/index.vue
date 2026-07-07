<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部" style="width:150px">
            <el-option label="待支付" value="pending_pay" />
            <el-option label="待发货" value="pending_ship" />
            <el-option label="已发货" value="shipped" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="toolbar"><span class="toolbar-title">订单管理</span></div>
    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="190" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }"><el-tag>{{ statusText[row.status] || row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="payAmount" label="订单金额" width="100" />
      <el-table-column label="本店商品" min-width="220">
        <template #default="{ row }">
          <div v-for="item in row.items" :key="item.productId" class="item-line">
            {{ item.productName }} x {{ item.quantity }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'pending_ship'" link type="success" @click="openShip(row)">发货</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" background
      @current-change="fetchData" @size-change="fetchData" />

    <el-dialog v-model="detailVisible" title="订单详情" width="720px">
      <el-descriptions v-if="currentOrder" :column="2" border>
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText[currentOrder.status] || currentOrder.status }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">{{ currentOrder.payAmount }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(currentOrder.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="currentOrder?.items || []" border style="margin-top:16px">
        <el-table-column prop="productName" label="商品" min-width="180" />
        <el-table-column prop="price" label="单价" width="90" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="totalPrice" label="小计" width="100" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="商家发货" width="420px">
      <el-form :model="shipForm" label-width="90px">
        <el-form-item label="物流公司"><el-input v-model="shipForm.logisticsCompany" /></el-form-item>
        <el-form-item label="物流单号"><el-input v-model="shipForm.logisticsNo" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible=false">取消</el-button>
        <el-button type="primary" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrderDetail, getOrderPage, shipOrder } from '@/api/merchant'

const statusText = {
  pending_pay: '待支付',
  pending_ship: '待发货',
  shipped: '已发货',
  completed: '已完成',
  cancelled: '已取消',
  refund_requested: '退款申请',
  refunded: '已退款'
}

const queryForm = reactive({ status: '', page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const detailVisible = ref(false)
const shipVisible = ref(false)
const currentOrder = ref(null)
const shipForm = reactive({ logisticsCompany: '', logisticsNo: '' })

const formatTime = (value) => value ? value.substring(0, 16) : '-'

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryForm }
    if (!params.status) delete params.status
    const res = await getOrderPage(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { queryForm.page = 1; fetchData() }
const handleReset = () => { Object.assign(queryForm, { status: '', page: 1, size: 10 }); fetchData() }

const openDetail = async (row) => {
  const res = await getOrderDetail(row.id)
  currentOrder.value = res.data
  detailVisible.value = true
}

const openShip = (row) => {
  currentOrder.value = row
  shipForm.logisticsCompany = ''
  shipForm.logisticsNo = ''
  shipVisible.value = true
}

const handleShip = async () => {
  await shipOrder(currentOrder.value.id, shipForm)
  ElMessage.success('已发货')
  shipVisible.value = false
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.item-line {
  line-height: 24px;
}
</style>
