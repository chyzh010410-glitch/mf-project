<template>
  <div class="page-container" v-loading="loading">
    <div class="back-bar">
      <el-button text :icon="ArrowLeft" @click="$router.back()">返回百科</el-button>
    </div>

    <div v-if="entry" class="detail-wrap">
      <el-carousel v-if="carouselImages.length" class="content-carousel" height="340px" indicator-position="outside">
        <el-carousel-item v-for="image in carouselImages" :key="image">
          <el-image :src="resolveImageUrl(image)" fit="cover" class="carousel-image" />
        </el-carousel-item>
      </el-carousel>
      <div v-else class="cover-wrap">
        <span style="font-size:64px">百科</span>
      </div>

      <h1 class="entry-name">{{ entry.name }}</h1>
      <p class="sci-name" v-if="entry.scientificName"><i>{{ entry.scientificName }}</i></p>
      <p class="alias-info" v-if="entry.alias">别名：{{ entry.alias }}</p>

      <div class="tags-row" v-if="entry.tags">
        <el-tag v-for="t in entry.tags.split(',').filter(Boolean)" :key="t" size="small" style="margin-right:6px">{{ t }}</el-tag>
        <span style="font-size:12px;color:#bbb;margin-left:8px">{{ entry.viewCount || 0 }} 次浏览</span>
      </div>

      <div class="info-grid">
        <div class="info-item" v-if="entry.family"><span class="label">科</span><span>{{ entry.family }}</span></div>
        <div class="info-item" v-if="entry.genus"><span class="label">属</span><span>{{ entry.genus }}</span></div>
        <div class="info-item" v-if="entry.pinyin"><span class="label">拼音</span><span>{{ entry.pinyin }}</span></div>
      </div>

      <el-divider />
      <div class="section" v-if="entry.description">
        <h3>简介</h3>
        <p>{{ entry.description }}</p>
      </div>
      <div class="section" v-if="entry.morphology">
        <h3>形态特征</h3>
        <p>{{ entry.morphology }}</p>
      </div>
      <div class="section" v-if="entry.distribution">
        <h3>分布</h3>
        <p>{{ entry.distribution }}</p>
      </div>
      <div class="section" v-if="entry.habitat">
        <h3>生长环境</h3>
        <p>{{ entry.habitat }}</p>
      </div>
      <div class="section" v-if="entry.careGuide">
        <h3>养护指南</h3>
        <p>{{ entry.careGuide }}</p>
      </div>
      <div class="section" v-if="entry.valueDescription">
        <h3>价值说明</h3>
        <p>{{ entry.valueDescription }}</p>
      </div>

      <el-divider />
      <div style="display:flex;align-items:center;gap:8px;margin-bottom:20px">
        <el-button :type="liked?'danger':''" :icon="liked?'StarFilled':'Star'" circle @click="handleLike" />
        <span style="font-size:14px;color:#666;margin-right:16px">{{ likeCount }} 人点赞</span>
        <el-button :type="favorited?'warning':''" :icon="favorited?'StarFilled':'Star'" circle @click="handleFavorite" />
        <span style="font-size:14px;color:#666">{{ favorited ? '已收藏' : '收藏' }}</span>
      </div>

      <div class="comment-section">
        <h3>评论 ({{ commentTotal }})</h3>
        <div style="display:flex;gap:10px;margin-bottom:20px">
          <el-input v-model="commentText" placeholder="写下你的评论..." style="flex:1" maxlength="500" show-word-limit />
          <el-button type="primary" @click="handlePostComment">发表</el-button>
        </div>
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <p class="comment-user">用户{{ c.userId }} <span style="color:#bbb;font-size:12px">{{ c.createTime?.substring(0,16) }}</span></p>
          <p class="comment-content">{{ c.content }}</p>
          <el-button link size="small" @click="handleReply(c)">回复</el-button>
          <el-button v-if="!repliesVisible[c.id]" link size="small" @click="toggleReplies(c.id)">展开回复</el-button>
          <div v-if="repliesVisible[c.id]" style="margin:8px 0 8px 32px">
            <div v-for="r in (subReplies[c.id]||[])" :key="r.id" style="margin-bottom:8px">
              <p style="font-size:13px;color:#888;margin:0">用户{{ r.userId }} 回复 <span style="color:#bbb">{{ r.createTime?.substring(0,16) }}</span></p>
              <p style="font-size:13px;margin:2px 0">{{ r.content }}</p>
            </div>
          </div>
          <div v-if="replyTo===c.id" style="display:flex;gap:8px;margin:8px 0 8px 32px">
            <el-input v-model="replyText" placeholder="回复..." size="small" style="flex:1" maxlength="300" />
            <el-button size="small" type="primary" @click="handlePostReply(c.id)">发送</el-button>
            <el-button size="small" @click="replyTo=null">取消</el-button>
          </div>
        </div>
        <el-pagination v-if="commentTotal>5" v-model:current-page="commentPage" :page-size="5" :total="commentTotal" layout="prev,pager,next" size="small" style="justify-content:center" @current-change="fetchComments" />
      </div>
    </div>

    <el-empty v-if="!loading && !entry" description="词条不存在" />
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getEncyclopediaDetail } from '@/api/encyclopedia'
import { getComments, getReplies, postComment } from '@/api/comment'
import { checkLike, toggleLike } from '@/api/like'
import { getFavorites, addFavorite, removeFavorite } from '@/api/favorite'
import { parseImageList, resolveImageUrl } from '@/utils/format'

const route = useRoute()
const entry = ref(null)
const loading = ref(false)

const carouselImages = computed(() => {
  if (!entry.value) return []
  const images = parseImageList(entry.value.images)
  return images.length ? images : (entry.value.coverImage ? [entry.value.coverImage] : [])
})

const liked = ref(false)
const likeCount = ref(0)
const fetchLike = async () => {
  try { const r=await checkLike('encyclopedia',Number(route.params.id)); if(r.code===200){ liked.value=r.data.liked; likeCount.value=r.data.count } } catch {}
}
const handleLike = async () => {
  try { const r=await toggleLike({targetType:'encyclopedia',targetId:Number(route.params.id)}); if(r.code===200){ liked.value=r.data.liked; liked.value?likeCount.value++:likeCount.value-- } } catch {}
}

const favorited = ref(false)
const favoriteId = ref(null)
const fetchFavorite = async () => {
  try { const r=await getFavorites({targetType:'encyclopedia',targetId:Number(route.params.id),page:1,size:1})
    if(r.code===200&&r.data&&r.data.records.length>0){ favorited.value=true; favoriteId.value=r.data.records[0].id } } catch {}
}
const handleFavorite = async () => {
  try {
    if(favorited.value){ await removeFavorite(favoriteId.value); favorited.value=false; favoriteId.value=null }
    else { const r=await addFavorite({targetType:'encyclopedia',targetId:Number(route.params.id)}); if(r.code===200&&r.data){ favorited.value=true; favoriteId.value=r.data.id } }
  } catch {}
}

const comments = ref([])
const commentPage = ref(1)
const commentTotal = ref(0)
const commentText = ref('')
const replyTo = ref(null)
const replyText = ref('')
const repliesVisible = ref({})
const subReplies = ref({})

const fetchComments = async () => {
  try {
    const r=await getComments({page:commentPage.value,size:5,targetType:'encyclopedia',targetId:route.params.id})
    if(r.code===200&&r.data){ comments.value=r.data.records||[]; commentTotal.value=r.data.total||0 }
  } catch {}
}
const handlePostComment = async () => {
  if(!commentText.value.trim()) return
  try {
    await postComment({targetType:'encyclopedia',targetId:Number(route.params.id),content:commentText.value})
    ElMessage.success('评论成功'); commentText.value=''; commentPage.value=1; fetchComments()
  } catch {}
}
const handleReply = (c) => { replyTo.value=c.id; replyText.value='' }
const handlePostReply = async (parentId) => {
  if(!replyText.value.trim()) return
  try {
    await postComment({targetType:'encyclopedia',targetId:Number(route.params.id),parentId,content:replyText.value})
    ElMessage.success('回复成功'); replyText.value=''; replyTo.value=null
    if(subReplies.value[parentId]!==undefined){ delete subReplies.value[parentId]; fetchReplies(parentId) }
  } catch {}
}
const fetchReplies = async (commentId) => {
  try { const r=await getReplies(commentId); if(r.code===200) subReplies.value[commentId]=r.data } catch {}
}
const toggleReplies = (commentId) => {
  if(repliesVisible.value[commentId]){ repliesVisible.value[commentId]=false; return }
  repliesVisible.value[commentId]=true; fetchReplies(commentId)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getEncyclopediaDetail(route.params.id)
    if (res.code === 200) entry.value = res.data
  } catch { /* ignore */ } finally { loading.value = false }
  fetchLike()
  fetchFavorite()
  fetchComments()
})
</script>

<style scoped>
.back-bar { margin-bottom: 16px; }
.detail-wrap { max-width: 860px; }
.cover-wrap { height: 300px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-bottom: 20px; background: #e8f5e9; color: #9ca3af; }
.content-carousel { border-radius: 12px; overflow: hidden; background: #f5f7f6; margin-bottom: 20px; }
.carousel-image { width: 100%; height: 340px; display: block; }
.entry-name { font-size: 28px; color: #1a1a1a; margin: 0 0 4px; }
.sci-name { font-size: 16px; color: #888; margin: 0 0 8px; }
.alias-info { font-size: 14px; color: #666; margin: 0 0 12px; }
.tags-row { display: flex; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 4px; }
.info-grid { display: flex; gap: 32px; flex-wrap: wrap; }
.info-item { font-size: 14px; color: #555; }
.info-item .label { display: inline-block; background: #f0f9f4; color: #2d8c4a; padding: 2px 10px; border-radius: 4px; margin-right: 8px; font-size: 12px; }
.section { margin-bottom: 24px; }
.section h3 { font-size: 16px; color: #2d8c4a; margin: 0 0 8px; padding-left: 10px; border-left: 3px solid #2d8c4a; }
.section p { font-size: 14px; color: #444; line-height: 1.8; white-space: pre-wrap; }
.comment-section h3 { font-size: 16px; color: #333; margin: 0 0 16px; }
.comment-item { border-bottom: 1px solid #f0f0f0; padding: 12px 0; }
.comment-user { font-size: 13px; color: #2d8c4a; margin: 0 0 4px; }
.comment-content { font-size: 14px; color: #333; margin: 4px 0; white-space: pre-wrap; }
</style>
