<template>
  <div class="page-container">
    <h2 class="section-title">优惠活动</h2>
    <p class="page-desc">查看平台当前可参与的促销活动和园艺福利。</p>

    <el-row v-loading="loading" :gutter="20" class="activity-grid">
      <el-col v-for="activity in activities" :key="activity.id" :xs="24" :sm="12" :md="8">
        <div class="activity-card" @click="openDetail(activity.id)">
          <div class="activity-cover" :style="coverStyle(activity)">
            <el-tag class="activity-type" type="success">{{ typeLabel(activity.type) }}</el-tag>
          </div>
          <div class="activity-body">
            <h3>{{ activity.title }}</h3>
            <p>{{ activity.description || '活动详情请查看规则说明。' }}</p>
            <div class="activity-time">{{ formatDate(activity.startTime) }} - {{ formatDate(activity.endTime) }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && activities.length === 0" description="暂无优惠活动" />

    <el-dialog v-model="detailVisible" title="活动详情" width="620px">
      <div v-if="detail" class="detail-panel">
        <div class="detail-cover" :style="coverStyle(detail)" />
        <h3>{{ detail.title }}</h3>
        <p>{{ detail.description || '暂无活动说明' }}</p>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="活动类型">{{ typeLabel(detail.type) }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatDateTime(detail.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ formatDateTime(detail.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="活动规则">{{ detail.ruleJson || '暂无规则说明' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getActivities, getActivityDetail } from '@/api/activity'
import { resolveImageUrl } from '@/utils/format'

const activities = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getActivities()
    if (res.code === 200) activities.value = res.data || []
  } catch {
    // request interceptor already shows the message
  } finally {
    loading.value = false
  }
}

const openDetail = async (id) => {
  const res = await getActivityDetail(id)
  if (res.code === 200) {
    detail.value = res.data
    detailVisible.value = true
  }
}

const coverStyle = (activity) => activity?.coverImage
  ? { backgroundImage: `url(${resolveImageUrl(activity.coverImage)})` }
  : {}

const typeLabel = (type) => {
  const labels = { discount: '折扣', coupon: '优惠券', full_reduction: '满减', banner: '专题' }
  return labels[type] || '活动'
}

const formatDate = (value) => value ? value.substring(0, 10) : '不限'
const formatDateTime = (value) => value ? value.replace('T', ' ').substring(0, 16) : '不限'

onMounted(fetchData)
</script>

<style scoped>
.page-desc { color: #7a7f87; margin: -4px 0 22px; }
.activity-grid { min-height: 260px; }
.activity-card { background: #fff; border-radius: 14px; overflow: hidden; border: 1px solid #ebeef5; margin-bottom: 20px; cursor: pointer; transition: transform .2s, box-shadow .2s; }
.activity-card:hover { transform: translateY(-4px); box-shadow: 0 12px 30px rgba(32, 77, 52, .12); }
.activity-cover { height: 160px; background: radial-gradient(circle at 20% 20%, #ffe7bd, transparent 34%), linear-gradient(135deg, #e8f5e9, #d9efe1); background-size: cover; background-position: center; position: relative; }
.activity-type { position: absolute; top: 12px; left: 12px; }
.activity-body { padding: 16px 18px; }
.activity-body h3 { margin: 0 0 8px; color: #25352b; font-size: 18px; }
.activity-body p { color: #66706a; line-height: 1.7; min-height: 48px; margin: 0 0 12px; }
.activity-time { font-size: 12px; color: #9ca3af; }
.detail-cover { height: 220px; border-radius: 12px; background: linear-gradient(135deg, #e8f5e9, #f7efe0); background-size: cover; background-position: center; margin-bottom: 16px; }
.detail-panel h3 { margin: 0 0 10px; font-size: 22px; color: #25352b; }
.detail-panel p { color: #66706a; line-height: 1.8; }
</style>
