<template>
  <div class="page-container">
    <h2 class="section-title">科普文章</h2>
    <el-row :gutter="20">
      <el-col :span="8" v-for="a in articles" :key="a.id" style="margin-bottom:20px">
        <div class="article-card" @click="$router.push('/article/'+a.id)">
          <div class="card-img" :style="coverStyle(a, '#f5f0e8')">
            <span v-if="!firstImage(a)" style="font-size:48px">文章</span>
            <div class="top-badge" v-if="a.isTop===1">置顶</div>
          </div>
          <div class="card-body">
            <h4>{{ a.title }}</h4>
            <p class="summary line-clamp-2" v-if="a.summary">{{ a.summary }}</p>
            <div class="card-footer">
              <el-tag size="small" v-for="t in (a.tags||'').split(',').filter(Boolean)" :key="t" style="margin-right:4px">{{ t }}</el-tag>
              <span class="views">{{ a.viewCount || 0 }} 次阅读</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && articles.length===0" description="暂无文章" />
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
import { getArticleList } from '@/api/article'
import { firstImage, resolveImageUrl } from '@/utils/format'

const articles = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(9)
const loading = ref(false)

const coverStyle = (record, fallback) => firstImage(record)
  ? { background: `url(${resolveImageUrl(firstImage(record))}) center/cover` }
  : { background: fallback }

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getArticleList({ page: page.value, size: size.value })
    if (res.code === 200 && res.data) {
      articles.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch { /* ignore */ } finally { loading.value = false }
}

onMounted(() => fetchData())
</script>

<style scoped>
.article-card {
  background: #fff; border-radius: 10px; overflow: hidden;
  cursor: pointer; transition: transform .2s, box-shadow .2s; height: 100%;
}
.article-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,.1); }
.card-img { height: 180px; display: flex; align-items: center; justify-content: center; position: relative; color: #9ca3af; }
.top-badge { position: absolute; top: 10px; left: 10px; background: #e74c3c; color: #fff; font-size: 12px; padding: 2px 10px; border-radius: 4px; }
.card-body { padding: 14px 16px; }
.summary { font-size: 13px; color: #666; margin: 8px 0 12px; }
.line-clamp-2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-footer { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.views { font-size: 12px; color: #bbb; margin-left: auto; }
</style>
