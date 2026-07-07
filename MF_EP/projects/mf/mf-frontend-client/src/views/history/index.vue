<template>
  <div class="page-container">
    <div class="page-head">
      <div>
        <h2 class="section-title">浏览历史</h2>
        <p class="page-desc">这里记录你最近浏览过的百科词条和科普文章，方便继续阅读。</p>
      </div>
      <el-button :disabled="items.length === 0" type="danger" plain @click="handleClear">清空历史</el-button>
    </div>

    <div v-loading="loading" class="history-list">
      <el-empty v-if="!loading && items.length === 0" description="暂无浏览历史" />

      <div v-for="item in items" :key="item.id" class="history-item" @click="goDetail(item)">
        <div class="history-cover" :style="coverStyle(item)">
          <span v-if="!item.targetImage">{{ typeLabel(item.targetType).slice(0, 1) }}</span>
        </div>
        <div class="history-main">
          <div class="history-meta">
            <el-tag size="small" :type="item.targetType === 'encyclopedia' ? 'success' : 'warning'">
              {{ typeLabel(item.targetType) }}
            </el-tag>
            <span>{{ formatTime(item.createTime) }}</span>
          </div>
          <h3>{{ item.targetName || '未命名内容' }}</h3>
          <p>停留 {{ formatDuration(item.stayDuration) }}</p>
        </div>
        <el-button link type="primary">继续查看</el-button>
      </div>
    </div>

    <el-pagination
      v-if="total > size"
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      background
      layout="prev,pager,next"
      class="pager"
      @current-change="fetchData"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearHistory, getHistory } from '@/api/history'
import { resolveImageUrl } from '@/utils/format'

const router = useRouter()
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getHistory({ page: page.value, size: size.value })
    if (res.code === 200 && res.data) {
      items.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    // request interceptor already shows the message
  } finally {
    loading.value = false
  }
}

const goDetail = (item) => {
  if (!item.targetId) return
  const base = item.targetType === 'encyclopedia' ? '/encyclopedia/' : '/article/'
  router.push(base + item.targetId)
}

const handleClear = () => {
  ElMessageBox.confirm('确认清空全部浏览历史？', '提示', { type: 'warning' }).then(async () => {
    await clearHistory()
    ElMessage.success('浏览历史已清空')
    page.value = 1
    fetchData()
  }).catch(() => {})
}

const typeLabel = (type) => {
  if (type === 'encyclopedia') return '百科'
  if (type === 'article') return '文章'
  return '内容'
}

const formatTime = (value) => value ? value.replace('T', ' ').substring(0, 16) : ''

const formatDuration = (seconds) => {
  if (!seconds) return '0 秒'
  if (seconds < 60) return `${seconds} 秒`
  return `${Math.floor(seconds / 60)} 分 ${seconds % 60} 秒`
}

const coverStyle = (item) => item.targetImage
  ? { backgroundImage: `url(${resolveImageUrl(item.targetImage)})` }
  : {}

onMounted(fetchData)
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; margin-bottom: 22px; }
.page-desc { color: #7a7f87; margin: 6px 0 0; font-size: 14px; }
.history-list { min-height: 260px; }
.history-item { display: flex; align-items: center; gap: 18px; background: #fff; border: 1px solid #ebeef5; border-radius: 12px; padding: 16px; margin-bottom: 12px; cursor: pointer; transition: transform .2s, box-shadow .2s; }
.history-item:hover { transform: translateY(-2px); box-shadow: 0 10px 28px rgba(32, 77, 52, .1); }
.history-cover { width: 92px; height: 68px; border-radius: 10px; background: linear-gradient(135deg, #e8f5e9, #f7efe0); background-size: cover; background-position: center; display: flex; align-items: center; justify-content: center; color: var(--color-primary); font-size: 24px; font-weight: 700; flex-shrink: 0; }
.history-main { flex: 1; min-width: 0; }
.history-meta { display: flex; align-items: center; gap: 10px; color: #a0a4aa; font-size: 12px; margin-bottom: 8px; }
.history-main h3 { margin: 0; font-size: 17px; color: #25352b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.history-main p { margin: 8px 0 0; color: #8b929b; font-size: 13px; }
.pager { justify-content: center; margin-top: 24px; }
@media (max-width: 640px) {
  .page-head { flex-direction: column; }
  .history-item { align-items: flex-start; }
  .history-cover { width: 76px; height: 60px; }
}
</style>
