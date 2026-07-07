<template>
  <div class="page-container">
    <el-row :gutter="24">
      <el-col :span="6">
        <div class="side-card">
          <div class="avatar-area">
            <div class="avatar-circle">{{ (user.nickname || user.username || '?')[0] }}</div>
            <h3>{{ user.nickname || user.username }}</h3>
            <p style="color:#999;font-size:13px">{{ user.phone }}</p>
          </div>
          <el-menu :default-active="$route.path" router style="border:none">
            <el-menu-item index="/user/profile"><el-icon><User /></el-icon> 个人资料</el-menu-item>
            <el-menu-item index="/user/addresses"><el-icon><MapLocation /></el-icon> 收货地址</el-menu-item>
            <el-menu-item index="/user/security"><el-icon><Lock /></el-icon> 账户安全</el-menu-item>
            <el-menu-item index="/user/points"><el-icon><Star /></el-icon> 我的积分</el-menu-item>
            <el-menu-item index="/orders"><el-icon><Document /></el-icon> 我的订单</el-menu-item>
            <el-menu-item index="/favorites"><el-icon><StarFilled /></el-icon> 我的收藏</el-menu-item>
            <el-menu-item index="/messages"><el-icon><Bell /></el-icon> 消息中心</el-menu-item>
          </el-menu>
        </div>
      </el-col>
      <el-col :span="18">
        <div class="main-card">
          <h3>个人资料</h3>
          <el-form :model="form" label-width="80px" style="max-width:400px;margin-top:20px">
            <el-form-item label="头像">
              <div class="avatar-circle small" style="margin-right:12px">{{ (form.nickname || '?')[0] }}</div>
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" />
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="user.username" disabled />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="user.phone" disabled />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="绑定邮箱" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :value="0">未知</el-radio>
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, MapLocation, Lock, Star, Document, StarFilled, Bell } from '@element-plus/icons-vue'
import { getProfile, updateProfile } from '@/api/user'

const user = ref({})
const form = reactive({ nickname: '', email: '', gender: 0 })

onMounted(async () => {
  try {
    const res = await getProfile()
    if (res.code === 200 && res.data) {
      user.value = res.data
      form.nickname = res.data.nickname || ''
      form.email = res.data.email || ''
      form.gender = res.data.gender || 0
    }
  } catch {}
})

const saveProfile = async () => {
  try {
    await updateProfile(form)
    user.value.nickname = form.nickname
    user.value.email = form.email
    ElMessage.success('保存成功')
  } catch {}
}
</script>

<style scoped>
.side-card { background: var(--color-white); border-radius: 8px; overflow: hidden; }
.avatar-area { text-align: center; padding: 30px 20px; background: linear-gradient(135deg, #e8f5e9, #fff); }
.avatar-circle { width: 72px; height: 72px; border-radius: 50%; background: var(--color-primary); color: #fff; font-size: 28px; display: flex; align-items: center; justify-content: center; margin: 0 auto 12px; }
.avatar-circle.small { width: 40px; height: 40px; font-size: 16px; }
.avatar-area h3 { margin-bottom: 4px; }
.main-card { background: var(--color-white); border-radius: 8px; padding: 24px; min-height: 400px; }
.main-card h3 { font-size: 18px; margin-bottom: 8px; }
</style>
