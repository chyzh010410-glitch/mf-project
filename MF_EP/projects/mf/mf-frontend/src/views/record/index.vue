<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">搜索</el-button>
          <el-button @click="queryForm.dateRange=null;fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar">
      <span class="toolbar-title">施肥记录</span>
      <el-button type="primary" @click="handleAdd">新增记录</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="id" label="ID" width="100" align="center" />
      <el-table-column prop="treeId" label="树木ID" width="100" align="center" />
      <el-table-column prop="fertilizerId" label="肥料ID" width="100" align="center" />
      <el-table-column prop="amount" label="用量" width="100" align="center" />
      <el-table-column prop="fertilizeDate" label="施肥日期" width="120" align="center" />
      <el-table-column prop="method" label="方式" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ {broadcast:'撒施',furrow:'沟施',foliar:'叶面',drip:'滴灌'}[row.method]||row.method }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper"
      background @current-change="fetchData" @size-change="fetchData"
    />

    <el-dialog v-model="dialogVisible" title="新增施肥记录" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="树木ID" prop="treeId">
          <el-input-number v-model="formData.treeId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="肥料ID" prop="fertilizerId">
          <el-input-number v-model="formData.fertilizerId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="用量" prop="amount">
          <el-input-number v-model="formData.amount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="施肥日期" prop="fertilizeDate">
          <el-date-picker v-model="formData.fertilizeDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="施肥方式" prop="method">
          <el-select v-model="formData.method" style="width:100%">
            <el-option label="撒施" value="broadcast" />
            <el-option label="沟施" value="furrow" />
            <el-option label="叶面喷施" value="foliar" />
            <el-option label="滴灌" value="drip" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRecordPage, saveRecord, deleteRecord } from '@/api/record'

const queryForm = reactive({ dateRange: null, page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)

const defaultForm = () => ({ treeId: null, fertilizerId: null, amount: 0, fertilizeDate: '', method: 'broadcast', remark: '' })
const formData = reactive(defaultForm())
const rules = {
  treeId: [{ required: true, message: '请输入树木ID', trigger: 'blur' }],
  fertilizerId: [{ required: true, message: '请输入肥料ID', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入用量', trigger: 'blur' }],
  fertilizeDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  method: [{ required: true, message: '请选择方式', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: queryForm.page, size: queryForm.size }
    if (queryForm.dateRange?.length === 2) {
      params.startDate = queryForm.dateRange[0]
      params.endDate = queryForm.dateRange[1]
    }
    const res = await getRecordPage(params)
    if (res.code === 200 && res.data) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } catch {} finally { loading.value = false }
}

const handleAdd = () => { Object.assign(formData, defaultForm()); dialogVisible.value = true }
const handleSave = async () => {
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    await saveRecord(formData)
    ElMessage.success('新增成功')
    dialogVisible.value = false; fetchData()
  } catch {} finally { saving.value = false }
}
const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该记录？', '删除确认', { type: 'warning' })
    .then(async () => { await deleteRecord(row.id); ElMessage.success('删除成功'); fetchData() })
    .catch(() => {})
}

onMounted(() => fetchData())
</script>
