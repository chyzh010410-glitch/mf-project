<template>
  <div class="page-container">
    <div class="summary-grid">
      <div class="summary-item">
        <span class="summary-label">当前店铺</span>
        <strong>{{ profile?.shopName || '-' }}</strong>
      </div>
      <div class="summary-item">
        <span class="summary-label">账号状态</span>
        <strong>{{ statusText[profile?.status] || '-' }}</strong>
      </div>
      <div class="summary-item">
        <span class="summary-label">联系人</span>
        <strong>{{ profile?.contactName || '-' }}</strong>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getProfile } from '@/api/merchant'

const profile = ref(null)
const statusText = { pending: '待审核', approved: '已通过', rejected: '已拒绝', disabled: '已禁用' }

onMounted(async () => {
  const res = await getProfile()
  profile.value = res.data
})
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.summary-item {
  padding: 18px;
  background: var(--color-primary-bg);
  border-radius: 6px;
}

.summary-label {
  display: block;
  margin-bottom: 8px;
  color: var(--color-text-secondary);
}

strong {
  font-size: 22px;
}
</style>
