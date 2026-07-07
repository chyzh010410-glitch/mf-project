<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="肥料名称">
          <el-input
            v-model="queryForm.name"
            placeholder="请输入名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="肥料类型">
          <el-select v-model="queryForm.type" placeholder="全部类型" clearable style="width: 160px">
            <el-option label="有机肥" value="organic" />
            <el-option label="复合肥" value="compound" />
            <el-option label="钾肥" value="potash" />
            <el-option label="氮肥" value="nitrogen" />
            <el-option label="磷肥" value="phosphate" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <span class="toolbar-title">肥料管理</span>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增肥料</el-button>
    </div>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column prop="id" label="ID" width="100" align="center" />
      <el-table-column prop="name" label="肥料名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="typeTagMap[row.type]">{{ typeMap[row.type] || row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="brand" label="品牌" width="120" show-overflow-tooltip />
      <el-table-column prop="nutrientContent" label="养分含量" width="120" align="center" show-overflow-tooltip />
      <el-table-column prop="unit" label="单位" width="80" align="center" />
      <el-table-column prop="stock" label="库存" width="100" align="center" />
      <el-table-column prop="unitPrice" label="单价(元)" width="100" align="center">
        <template #default="{ row }">
          {{ row.unitPrice != null ? formatAmount(row.unitPrice) : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑肥料' : '新增肥料'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="肥料名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入肥料名称" />
        </el-form-item>
        <el-form-item label="肥料类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="有机肥" value="organic" />
            <el-option label="复合肥" value="compound" />
            <el-option label="钾肥" value="potash" />
            <el-option label="氮肥" value="nitrogen" />
            <el-option label="磷肥" value="phosphate" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="formData.brand" placeholder="请输入品牌" />
        </el-form-item>
        <el-form-item label="养分含量" prop="nutrientContent">
          <el-input v-model="formData.nutrientContent" placeholder="如 N-P2O5-K2O: 15-15-15" />
        </el-form-item>
        <el-form-item label="计量单位" prop="unit">
          <el-select v-model="formData.unit" placeholder="请选择单位" style="width: 100%">
            <el-option label="千克 (kg)" value="kg" />
            <el-option label="吨 (t)" value="t" />
            <el-option label="升 (L)" value="L" />
            <el-option label="袋" value="bag" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存数量" prop="stock">
          <el-input-number v-model="formData.stock" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单价(元)" prop="unitPrice">
          <el-input-number v-model="formData.unitPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="选填" />
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
  getFertilizerPage, getFertilizerById,
  saveFertilizer, updateFertilizer, deleteFertilizer
} from '@/api/fertilizer'
import { formatAmount } from '@/utils/format'

const typeMap = {
  organic: '有机肥',
  compound: '复合肥',
  potash: '钾肥',
  nitrogen: '氮肥',
  phosphate: '磷肥'
}

const typeTagMap = {
  organic: 'success',
  compound: 'warning',
  potash: 'info',
  nitrogen: '',
  phosphate: 'danger'
}

// 查询参数
const queryForm = reactive({ name: '', type: '', page: 1, size: 10 })

// 表格数据
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// 弹窗数据
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editId = ref(null)

const defaultForm = () => ({
  name: '',
  type: '',
  brand: '',
  nutrientContent: '',
  unit: 'kg',
  stock: 0,
  unitPrice: 0,
  remark: ''
})

const formData = reactive(defaultForm())

const formRules = {
  name: [{ required: true, message: '请输入肥料名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择肥料类型', trigger: 'change' }],
  unit: [{ required: true, message: '请选择计量单位', trigger: 'change' }]
}

// 获取列表数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getFertilizerPage(queryForm)
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  queryForm.page = 1
  fetchData()
}

// 重置
const handleReset = () => {
  queryForm.name = ''
  queryForm.type = ''
  queryForm.page = 1
  fetchData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  Object.assign(formData, defaultForm())
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row) => {
  isEdit.value = true
  editId.value = row.id
  const res = await getFertilizerById(row.id)
  if (res.code === 200 && res.data) {
    Object.assign(formData, {
      name: res.data.name || '',
      type: res.data.type || '',
      brand: res.data.brand || '',
      nutrientContent: res.data.nutrientContent || '',
      unit: res.data.unit || 'kg',
      stock: res.data.stock ?? 0,
      unitPrice: res.data.unitPrice ?? 0,
      remark: res.data.remark || ''
    })
  }
  dialogVisible.value = true
}

// 保存
const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await updateFertilizer({ id: editId.value, ...formData })
      ElMessage.success('更新成功')
    } else {
      await saveFertilizer(formData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // 拦截器已处理
  } finally {
    saving.value = false
  }
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确认删除肥料「${row.name}」？删除后不可恢复。`,
    '删除确认',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    await deleteFertilizer(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => { /* 取消 */ })
}

onMounted(() => {
  fetchData()
})
</script>
