<template>
  <div class="page-container">
    <el-form :model="form" label-width="90px" style="max-width:520px">
      <el-form-item label="用户名"><el-input v-model="form.username" disabled /></el-form-item>
      <el-form-item label="店铺名称"><el-input v-model="form.shopName" /></el-form-item>
      <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="状态"><el-tag>{{ statusText[form.status] || form.status }}</el-tag></el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '@/api/merchant'

const saving = ref(false)
const statusText = { pending: '待审核', approved: '已通过', rejected: '已拒绝', disabled: '已禁用' }
const form = reactive({ username: '', shopName: '', contactName: '', phone: '', status: '' })

const loadProfile = async () => {
  const res = await getProfile()
  Object.assign(form, res.data || {})
}

const handleSave = async () => {
  saving.value = true
  try {
    await updateProfile({ shopName: form.shopName, contactName: form.contactName, phone: form.phone })
    ElMessage.success('已保存')
    loadProfile()
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>
