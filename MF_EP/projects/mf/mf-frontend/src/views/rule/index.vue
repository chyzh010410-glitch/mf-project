<template>
  <div class="page-container">
    <div class="toolbar">
      <span class="toolbar-title">施肥规则管理</span>
      <el-button type="primary" @click="handleAdd">新增规则</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="species" label="适用树种" width="140" />
      <el-table-column prop="ageMin" label="树龄范围" width="140" align="center">
        <template #default="{ row }">{{ row.ageMin }} - {{ row.ageMax }} 年</template>
      </el-table-column>
      <el-table-column prop="season" label="季节" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ {spring:'春季',summer:'夏季',autumn:'秋季',winter:'冬季',all:'四季'}[row.season] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fertilizerId" label="推荐肥料ID" width="110" align="center" />
      <el-table-column prop="recommendAmount" label="推荐用量" width="100" align="center" />
      <el-table-column prop="method" label="施肥方式" width="100" align="center">
        <template #default="{ row }">
          {{ {broadcast:'撒施',furrow:'沟施',foliar:'叶面',drip:'滴灌'}[row.method]||row.method }}
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="80" align="center" />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑规则':'新增规则'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="适用树种" prop="species">
          <el-input v-model="formData.species" placeholder="如 苹果、柑橘" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最小树龄" prop="ageMin">
              <el-input-number v-model="formData.ageMin" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大树龄" prop="ageMax">
              <el-input-number v-model="formData.ageMax" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="适用季节" prop="season">
          <el-select v-model="formData.season" style="width:100%">
            <el-option label="春季" value="spring" />
            <el-option label="夏季" value="summer" />
            <el-option label="秋季" value="autumn" />
            <el-option label="冬季" value="winter" />
            <el-option label="四季通用" value="all" />
          </el-select>
        </el-form-item>
        <el-form-item label="推荐肥料ID" prop="fertilizerId">
          <el-input-number v-model="formData.fertilizerId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="推荐用量" prop="recommendAmount">
          <el-input-number v-model="formData.recommendAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="施肥方式" prop="method">
          <el-select v-model="formData.method" style="width:100%">
            <el-option label="撒施" value="broadcast" />
            <el-option label="沟施" value="furrow" />
            <el-option label="叶面喷施" value="foliar" />
            <el-option label="滴灌" value="drip" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="formData.priority" :min="0" style="width:100%" />
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
import { getRulePage, getRuleById, saveRule, updateRule, deleteRule } from '@/api/rule'

const queryForm = reactive({ page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editId = ref(null)

const defaultForm = () => ({ species: '', ageMin: 0, ageMax: 20, season: 'all', fertilizerId: null, recommendAmount: 0, method: 'broadcast', priority: 1, remark: '' })
const formData = reactive(defaultForm())
const rules = {
  species: [{ required: true, message: '请输入树种', trigger: 'blur' }],
  season: [{ required: true, message: '请选择季节', trigger: 'change' }],
  fertilizerId: [{ required: true, message: '请输入肥料ID', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getRulePage(queryForm)
    if (res.code === 200 && res.data) { tableData.value = res.data.records || []; total.value = res.data.total || 0 }
  } catch {} finally { loading.value = false }
}

const handleAdd = () => { isEdit.value = false; editId.value = null; Object.assign(formData, defaultForm()); dialogVisible.value = true }
const handleEdit = async (row) => {
  isEdit.value = true; editId.value = row.id
  const res = await getRuleById(row.id)
  if (res.code === 200 && res.data) Object.assign(formData, res.data)
  dialogVisible.value = true
}
const handleSave = async () => {
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    isEdit.value ? await updateRule({ id: editId.value, ...formData }) : await saveRule(formData)
    ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
    dialogVisible.value = false; fetchData()
  } catch {} finally { saving.value = false }
}
const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除规则 #${row.id}？`, '删除确认', { type: 'warning' })
    .then(async () => { await deleteRule(row.id); ElMessage.success('删除成功'); fetchData() })
    .catch(() => {})
}

onMounted(() => fetchData())
</script>
