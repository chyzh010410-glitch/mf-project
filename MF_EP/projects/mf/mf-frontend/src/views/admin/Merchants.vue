<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="用户名/店铺/手机号" clearable @keyup.enter="handleSearch" style="width:220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width:130px">
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="已禁用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar"><span class="toolbar-title">商家列表</span></div>
    <el-table v-loading="loading" :data="tableData" border stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="shopName" label="店铺名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="contactName" label="联系人" width="100" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMeta[row.status]?.type || 'info'">{{ statusMeta[row.status]?.label || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditRemark" label="审核备注" min-width="140" show-overflow-tooltip />
      <el-table-column prop="createTime" label="申请时间" width="160" align="center">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending' || row.status === 'rejected'" link type="success" @click="handleApprove(row)">通过</el-button>
          <el-button v-if="row.status === 'pending'" link type="warning" @click="openReject(row)">拒绝</el-button>
          <el-button v-if="row.status !== 'disabled'" link type="danger" @click="handleDisable(row)">禁用</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" background
      @current-change="fetchData" @size-change="fetchData" />

    <el-dialog v-model="rejectDialogVisible" title="审核拒绝" width="420px">
      <el-input v-model="rejectRemark" type="textarea" :rows="4" placeholder="请输入审核备注" />
      <template #footer>
        <el-button @click="rejectDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { approveMerchant, disableMerchant, getMerchantPage, rejectMerchant } from '@/api/admin'

const statusMeta = {
  pending: { label: '待审核', type: 'warning' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已拒绝', type: 'danger' },
  disabled: { label: '已禁用', type: 'info' }
}

const queryForm = reactive({ keyword: '', status: '', page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const rejectDialogVisible = ref(false)
const rejectRemark = ref('')
const currentMerchant = ref(null)

const formatTime = (value) => value ? value.substring(0, 16) : '-'

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryForm }
    if (!params.keyword) delete params.keyword
    if (!params.status) delete params.status
    const res = await getMerchantPage(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.status = ''
  queryForm.page = 1
  fetchData()
}

const handleApprove = async (row) => {
  await approveMerchant(row.id)
  ElMessage.success('审核已通过')
  fetchData()
}

const openReject = (row) => {
  currentMerchant.value = row
  rejectRemark.value = ''
  rejectDialogVisible.value = true
}

const handleReject = async () => {
  if (!currentMerchant.value) return
  await rejectMerchant(currentMerchant.value.id, rejectRemark.value)
  ElMessage.success('已拒绝')
  rejectDialogVisible.value = false
  fetchData()
}

const handleDisable = async (row) => {
  await ElMessageBox.confirm(`确认禁用商家「${row.shopName}」？`, '提示', { type: 'warning' })
  await disableMerchant(row.id)
  ElMessage.success('已禁用')
  fetchData()
}

onMounted(fetchData)
</script>
