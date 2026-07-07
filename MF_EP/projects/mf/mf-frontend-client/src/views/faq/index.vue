<template>
  <div class="page-container">
    <h2 class="section-title">帮助中心</h2>
    <p class="page-desc">常见问题按分类整理，优先解决购物、订单和账号使用中的高频问题。</p>

    <div class="faq-toolbar">
      <el-radio-group v-model="activeCategory" @change="fetchData">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button v-for="category in categories" :key="category" :label="category">
          {{ category }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="faq-list">
      <el-empty v-if="!loading && faqs.length === 0" description="暂无常见问题" />
      <el-collapse v-else accordion>
        <el-collapse-item v-for="faq in faqs" :key="faq.id" :name="faq.id">
          <template #title>
            <div class="faq-title">
              <el-tag size="small" type="success">{{ faq.category || '通用' }}</el-tag>
              <span>{{ faq.question }}</span>
            </div>
          </template>
          <div class="faq-answer">{{ faq.answer }}</div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getFaqList } from '@/api/faq'

const faqs = ref([])
const activeCategory = ref('')
const loading = ref(false)

const categories = computed(() => {
  const values = faqs.value.map(item => item.category).filter(Boolean)
  return [...new Set(values)]
})

const fetchData = async () => {
  loading.value = true
  try {
    const params = activeCategory.value ? { category: activeCategory.value } : {}
    const res = await getFaqList(params)
    if (res.code === 200) faqs.value = res.data || []
  } catch {
    // request interceptor already shows the message
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page-desc { color: #7a7f87; margin: -4px 0 22px; }
.faq-toolbar { background: #fff; border: 1px solid #ebeef5; border-radius: 12px; padding: 14px; margin-bottom: 18px; overflow-x: auto; }
.faq-list { min-height: 260px; background: #fff; border-radius: 12px; padding: 8px 18px; border: 1px solid #ebeef5; }
.faq-title { display: flex; align-items: center; gap: 12px; font-size: 15px; color: #25352b; }
.faq-answer { color: #66706a; line-height: 1.9; padding: 0 6px 14px 48px; white-space: pre-wrap; }
@media (max-width: 640px) {
  .faq-answer { padding-left: 0; }
}
</style>
