<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="树种">
          <el-input v-model="queryForm.species" placeholder="请输入树种" clearable />
        </el-form-item>
        <el-form-item label="健康状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="健康" value="healthy" />
            <el-option label="病害" value="sick" />
            <el-option label="死亡" value="dead" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">搜索</el-button>
          <el-button @click="queryForm.species='';queryForm.status='';fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar">
      <span class="toolbar-title">树木管理</span>
      <el-button type="primary" @click="handleAdd">新增树木</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="id" label="ID" width="100" align="center" />
      <el-table-column prop="species" label="树种" min-width="120" />
      <el-table-column prop="variety" label="品种" min-width="120" />
      <el-table-column prop="age" label="树龄(年)" width="100" align="center" />
      <el-table-column prop="area" label="面积(㎡)" width="100" align="center" />
      <el-table-column prop="quantity" label="数量" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status==='healthy'?'success':row.status==='sick'?'warning':'danger'" size="small">
            {{ row.status==='healthy'?'健康':row.status==='sick'?'病害':'死亡' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper"
      background @current-change="fetchData" @size-change="fetchData"
    />

    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑树木':'新增树木'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="树种" prop="species">
          <el-input v-model="formData.species" placeholder="请输入树种名称" />
        </el-form-item>
        <el-form-item label="品种" prop="variety">
          <el-input v-model="formData.variety" placeholder="请输入品种" />
        </el-form-item>
        <el-form-item label="树龄" prop="age">
          <el-input-number v-model="formData.age" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="种植面积(㎡)" prop="area">
          <el-input-number v-model="formData.area" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="formData.quantity" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="健康状态" prop="status">
          <el-select v-model="formData.status" style="width:100%">
            <el-option label="健康" value="healthy" />
            <el-option label="病害" value="sick" />
            <el-option label="死亡" value="dead" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" />
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
import { getTreePage, getTreeById, saveTree, updateTree, deleteTree } from '@/api/tree'

const queryForm = reactive({ species: '', status: '', page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editId = ref(null)

const defaultForm = () => ({ species: '', variety: '', age: 0, area: 0, quantity: 0, status: 'healthy', remark: '' })
const formData = reactive(defaultForm())
const rules = {
  species: [{ required: true, message: '请输入树种', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTreePage(queryForm)
    if (res.code === 200 && res.data) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } catch {} finally { loading.value = false }
}

const handleAdd = () => { isEdit.value = false; editId.value = null; Object.assign(formData, defaultForm()); dialogVisible.value = true }
const handleEdit = async (row) => {
  isEdit.value = true; editId.value = row.id
  const res = await getTreeById(row.id)
  if (res.code === 200 && res.data) Object.assign(formData, res.data)
  dialogVisible.value = true
}
const handleSave = async () => {
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    isEdit.value ? await updateTree({ id: editId.value, ...formData }) : await saveTree(formData)
    ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
    dialogVisible.value = false; fetchData()
  } catch {} finally { saving.value = false }
}
const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除树木「${row.species}」？`, '删除确认', { type: 'warning' })
    .then(async () => { await deleteTree(row.id); ElMessage.success('删除成功'); fetchData() })
    .catch(() => {})
}

onMounted(() => fetchData())
</script>
