<template>
  <div class="page-container">
    <h2 class="section-title">我的收藏</h2>
    <el-tabs v-model="tab" @tab-change="fetchData">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="百科" name="encyclopedia" />
      <el-tab-pane label="文章" name="article" />
    </el-tabs>
    <div v-if="items.length===0&&!loading" style="padding:80px;text-align:center;color:#999">暂无收藏</div>
    <div v-for="f in items" :key="f.id" class="fav-item">
      <div class="fav-left" @click="goDetail(f)">
        <el-tag size="small" :type="f.targetType==='encyclopedia'?'success':''">
          {{ f.targetType==='encyclopedia'?'百科':'文章' }}
        </el-tag>
        <span class="fav-name">{{ f.targetType==='encyclopedia'?'百科词条':'文章' }} #{{ f.targetId }}</span>
        <span class="fav-time">{{ f.createTime?.substring(0,10) }}</span>
      </div>
      <el-button type="danger" link size="small" @click.stop="handleRemove(f)">取消收藏</el-button>
    </div>
    <el-pagination v-if="total>size" v-model:current-page="page" :page-size="size" :total="total"
      :page-sizes="[10,20]" background layout="prev,pager,next" style="justify-content:center;margin-top:24px"
      @current-change="fetchData" @size-change="fetchData" />
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { getFavorites, removeFavorite } from '@/api/favorite'

const router = useRouter()
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const tab = ref('')
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (tab.value) params.targetType = tab.value
    const r = await getFavorites(params)
    if (r.code === 200 && r.data) {
      items.value = r.data.records || []
      total.value = r.data.total || 0
    }
  } catch {} finally { loading.value = false }
}

const goDetail = (f) => {
  const path = f.targetType === 'encyclopedia' ? '/encyclopedia/' : '/article/'
  router.push(path + f.targetId)
}

const handleRemove = (f) => {
  ElMessageBox.confirm('确认取消收藏？', '提示', { type: 'warning' }).then(async () => {
    await removeFavorite(f.id)
    ElMessage.success('已取消收藏')
    fetchData()
  }).catch(() => {})
}

onMounted(fetchData)
</script>
<style scoped>
.fav-item { display: flex; align-items: center; justify-content: space-between; background: #fff; border-radius: 8px; padding: 14px 20px; margin-bottom: 10px; border: 1px solid #ebeef5; }
.fav-left { flex: 1; display: flex; align-items: center; gap: 12px; cursor: pointer; }
.fav-name { font-size: 14px; color: #333; }
.fav-time { font-size: 12px; color: #bbb; margin-left: auto; }
</style>
