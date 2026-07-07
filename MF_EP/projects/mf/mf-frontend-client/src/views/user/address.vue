<template>
  <div class="page-container">
    <h2 class="section-title">收货地址</h2>
    <div style="margin-bottom:16px">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增地址</el-button>
    </div>
    <div v-if="addresses.length===0 && !loading" style="padding:80px;text-align:center;color:#999">暂无收货地址</div>
    <div v-for="addr in addresses" :key="addr.id" style="background:#fff;border-radius:8px;padding:16px 20px;margin-bottom:12px;border:1px solid #ebeef5;display:flex;justify-content:space-between;align-items:center">
      <div>
        <div style="margin-bottom:6px">
          <strong>{{ addr.receiverName }}</strong>
          <span style="color:#999;margin-left:12px">{{ addr.receiverPhone }}</span>
          <el-tag v-if="addr.isDefault===1" size="small" type="danger" style="margin-left:8px">默认</el-tag>
        </div>
        <p style="color:#666;margin:0;font-size:13px">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</p>
      </div>
      <div style="display:flex;gap:8px;flex-shrink:0">
        <el-button size="small" @click="handleEdit(addr)">编辑</el-button>
        <el-button v-if="addr.isDefault!==1" size="small" type="success" @click="handleSetDefault(addr)">设为默认</el-button>
        <el-button size="small" type="danger" @click="handleDelete(addr)">删除</el-button>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑地址':'新增地址'" width="480px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="收货人" prop="receiverName"><el-input v-model="formData.receiverName" placeholder="请输入收货人姓名" /></el-form-item>
        <el-form-item label="手机号" prop="receiverPhone"><el-input v-model="formData.receiverPhone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="所在地区" prop="province">
          <el-row :gutter="8">
            <el-col :span="8"><el-input v-model="formData.province" placeholder="省" /></el-col>
            <el-col :span="8"><el-input v-model="formData.city" placeholder="市" /></el-col>
            <el-col :span="8"><el-input v-model="formData.district" placeholder="区" /></el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="详细地址" prop="detail"><el-input v-model="formData.detail" type="textarea" :rows="2" placeholder="街道/门牌号等" /></el-form-item>
        <el-form-item label="设为默认"><el-switch v-model="formData.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/address'

const addresses = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editId = ref(null)

const defaultForm = () => ({
  receiverName: '', receiverPhone: '',
  province: '', city: '', district: '',
  detail: '', isDefault: 0
})
const formData = reactive(defaultForm())
const rules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAddresses()
    if (res.code === 200) addresses.value = res.data || []
  } catch {} finally { loading.value = false }
}

const handleAdd = () => {
  isEdit.value = false; editId.value = null
  Object.assign(formData, defaultForm())
  dialogVisible.value = true
}

const handleEdit = (addr) => {
  isEdit.value = true; editId.value = addr.id
  Object.assign(formData, {
    receiverName: addr.receiverName || '', receiverPhone: addr.receiverPhone || '',
    province: addr.province || '', city: addr.city || '', district: addr.district || '',
    detail: addr.detail || '', isDefault: addr.isDefault || 0
  })
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    isEdit.value ? await updateAddress(editId.value, formData) : await addAddress(formData)
    ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
    dialogVisible.value = false; fetchList()
  } catch {} finally { saving.value = false }
}

const handleDelete = (addr) => {
  ElMessageBox.confirm('确认删除该地址？', '删除确认', { type:'warning' }).then(async () => {
    await deleteAddress(addr.id)
    ElMessage.success('已删除'); fetchList()
  }).catch(() => {})
}

const handleSetDefault = async (addr) => {
  await setDefaultAddress(addr.id)
  ElMessage.success('已设为默认地址'); fetchList()
}

onMounted(fetchList)
</script>
