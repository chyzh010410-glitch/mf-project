<template>
  <div class="page-container">
    <h2 class="section-title">账户安全</h2>
    <el-card style="max-width:480px">
      <template #header><span>修改密码</span></template>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="formData.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="formData.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="formData.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSubmit">确认修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/user'

const formRef = ref(null)
const saving = ref(false)
const formData = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirm = (_rule, value, callback) => {
  if (value !== formData.newPassword) callback(new Error('两次密码输入不一致'))
  else callback()
}

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码不少于6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }, { validator: validateConfirm, trigger: 'blur' }]
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const res = await changePassword({ oldPassword: formData.oldPassword, newPassword: formData.newPassword })
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      localStorage.removeItem('clientToken')
      setTimeout(() => window.location.href = '/login', 1500)
    }
  } catch {} finally { saving.value = false }
}
</script>
