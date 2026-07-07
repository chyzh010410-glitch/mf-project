<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="分类名称">
          <el-input v-model="queryForm.name" placeholder="请输入名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类类型">
          <el-select v-model="queryForm.type" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="树苗" value="tree" />
            <el-option label="化肥" value="fertilizer" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar">
      <span class="toolbar-title">商品分类</span>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增分类</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="分类名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'tree' ? 'success' : 'warning'">
            {{ row.type === 'tree' ? '树苗' : '化肥' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="parentId" label="父级ID" width="80" align="center" />
      <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
      <el-table-column prop="icon" label="图标" width="100" align="center" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="queryForm.page"
      v-model:page-size="queryForm.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @current-change="fetchData"
      @size-change="fetchData"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="树苗" value="tree" />
            <el-option label="化肥" value="fertilizer" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级ID">
          <el-input-number v-model="formData.parentId" :min="0" style="width: 100%" placeholder="0 表示顶级分类" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="formData.icon" placeholder="图标 class 或 URL" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getCategoryPage, getCategoryDetail,
  saveCategory, updateCategory, deleteCategory
} from '@/api/admin'

const queryForm = reactive({ name: '', type: '', page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editId = ref(null)

const defaultForm = () => ({
  name: '',
  type: '',
  parentId: 0,
  sortOrder: 0,
  icon: '',
  description: ''
})

const formData = reactive(defaultForm())

const formRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择分类类型', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCategoryPage(queryForm)
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch { /* ignore */ } finally { loading.value = false }
}

const handleSearch = () => { queryForm.page = 1; fetchData() }

const handleReset = () => {
  queryForm.name = ''
  queryForm.type = ''
  queryForm.page = 1
  fetchData()
}

const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  Object.assign(formData, defaultForm())
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  editId.value = row.id
  try {
    const res = await getCategoryDetail(row.id)
    if (res.code === 200 && res.data) {
      Object.assign(formData, {
        name: res.data.name || '',
        type: res.data.type || '',
        parentId: res.data.parentId ?? 0,
        sortOrder: res.data.sortOrder ?? 0,
        icon: res.data.icon || '',
        description: res.data.description || ''
      })
    }
  } catch { /* ignore */ }
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await updateCategory(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await saveCategory(formData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch { /* ignore */ } finally { saving.value = false }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确认删除分类「${row.name}」？`,
    '删除确认',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

onMounted(() => {
  fetchData()
})
</script>
