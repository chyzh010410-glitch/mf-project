<template>
  <div class="page-container">
    <h2 class="section-title">树木百科</h2>
    <div style="max-width:400px;margin-bottom:24px">
      <el-input v-model="keyword" placeholder="搜索树木名称、学名..." clearable @keyup.enter="handleSearch" @clear="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>
    <el-row :gutter="20">
      <el-col :span="8" v-for="entry in entries" :key="entry.id" style="margin-bottom:20px">
        <div class="entry-card" @click="$router.push('/encyclopedia/'+entry.id)">
          <div class="card-img" :style="coverStyle(entry, '#e8f5e9')">
            <span v-if="!firstImage(entry)" style="font-size:48px">百科</span>
          </div>
          <div class="card-body">
            <h4>{{ entry.name }}</h4>
            <p class="sci-name" v-if="entry.scientificName"><i>{{ entry.scientificName }}</i></p>
            <p class="meta">{{ entry.family || '' }} {{ entry.genus || '' }}</p>
            <p class="desc line-clamp-2" v-if="entry.description">{{ entry.description }}</p>
            <div class="card-footer">
              <el-tag size="small" v-for="t in (entry.tags||'').split(',').filter(Boolean)" :key="t" style="margin-right:4px">{{ t }}</el-tag>
              <span class="views">{{ entry.viewCount || 0 }} 次浏览</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && entries.length===0" description="暂无百科词条" />
    <el-pagination
      v-if="total > size"
      v-model:current-page="page" :page-size="size" :total="total"
      background layout="prev,pager,next" style="justify-content:center;margin-top:24px"
      @current-change="fetchData"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getEncyclopediaList } from '@/api/encyclopedia'
import { firstImage, resolveImageUrl } from '@/utils/format'

const entries = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(9)
const keyword = ref('')
const loading = ref(false)

const coverStyle = (record, fallback) => firstImage(record)
  ? { background: `url(${resolveImageUrl(firstImage(record))}) center/cover` }
  : { background: fallback }

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getEncyclopediaList({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    if (res.code === 200 && res.data) {
      entries.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch { /* ignore */ } finally { loading.value = false }
}

const handleSearch = () => { page.value = 1; fetchData() }

onMounted(() => fetchData())
</script>

<style scoped>
.entry-card {
  background: #fff; border-radius: 10px; overflow: hidden;
  cursor: pointer; transition: transform .2s, box-shadow .2s; height: 100%;
}
.entry-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,.1); }
.card-img { height: 180px; display: flex; align-items: center; justify-content: center; color: #9ca3af; }
.card-body { padding: 14px 16px; }
.sci-name { font-size: 13px; color: #888; margin: 2px 0; }
.meta { font-size: 12px; color: #999; margin: 4px 0; }
.desc { font-size: 13px; color: #666; margin: 8px 0; }
.line-clamp-2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-footer { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; margin-top: 8px; }
.views { font-size: 12px; color: #bbb; margin-left: auto; }
</style>
