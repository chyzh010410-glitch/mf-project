<template>
  <div class="page-container">
    <div class="page-head">
      <div>
        <h2 class="section-title">商品商城</h2>
        <p class="page-desc">选择树苗和肥料，按销量、价格快速筛选适合的园艺商品。</p>
      </div>
      <el-select v-model="sort" class="sort-select" @change="handleSearch">
        <el-option label="默认排序" value="" />
        <el-option label="销量优先" value="sales" />
        <el-option label="价格从低到高" value="price_asc" />
        <el-option label="价格从高到低" value="price_desc" />
      </el-select>
    </div>

    <el-tabs v-model="productType" @tab-change="handleSearch">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="树苗" name="tree" />
      <el-tab-pane label="肥料" name="fertilizer" />
    </el-tabs>

    <el-row v-loading="loading" :gutter="18" class="product-grid">
      <el-col v-for="product in products" :key="product.id" :xs="12" :sm="8" :md="6">
        <div class="product-card" @click="router.push('/product/' + product.id)">
          <div class="product-cover" :style="coverStyle(product)">
            <span v-if="!firstImage(product)">{{ typeLabel(product.productType) }}</span>
          </div>
          <div class="product-body">
            <h3>{{ product.name }}</h3>
            <p>{{ product.brand || '平台精选' }}</p>
            <div class="product-footer">
              <span class="price">{{ formatCurrency(product.price) }}</span>
              <span class="sales">已售 {{ product.salesCount || 0 }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && products.length === 0" description="暂无商品" />

    <el-pagination
      v-if="total > size"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[12, 24]"
      background
      layout="prev,pager,next,sizes"
      class="pager"
      @current-change="fetchList"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductList } from '@/api/product'
import { firstImage, formatCurrency, resolveImageUrl } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const products = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const productType = ref(route.query.productType || '')
const sort = ref('')
const loading = ref(false)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getProductList({
      page: page.value,
      size: size.value,
      productType: productType.value || undefined,
      keyword: route.query.keyword || undefined,
      sort: sort.value || undefined
    })
    if (res.code === 200 && res.data) {
      products.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    // request interceptor already shows the message
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchList()
}

const handleSizeChange = () => {
  page.value = 1
  fetchList()
}

const coverStyle = (product) => firstImage(product)
  ? { backgroundImage: `url(${resolveImageUrl(firstImage(product))})` }
  : {}

const typeLabel = (type) => type === 'fertilizer' ? '肥料' : '树苗'

watch(() => route.query.keyword, handleSearch)
onMounted(fetchList)
</script>

<style scoped>
.page-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.page-desc { color: #7a7f87; margin: 6px 0 0; font-size: 14px; }
.sort-select { width: 160px; }
.product-grid { min-height: 260px; }
.product-card { background: #fff; border: 1px solid #ebeef5; border-radius: 12px; overflow: hidden; cursor: pointer; margin-bottom: 18px; transition: transform .2s, box-shadow .2s; height: calc(100% - 18px); }
.product-card:hover { transform: translateY(-4px); box-shadow: 0 12px 28px rgba(32, 77, 52, .12); }
.product-cover { height: 190px; background: linear-gradient(135deg, #e8f5e9, #f7efe0); background-size: cover; background-position: center; display: flex; align-items: center; justify-content: center; color: var(--color-primary); font-size: 24px; font-weight: 700; }
.product-body { padding: 14px; }
.product-body h3 { margin: 0; color: #25352b; font-size: 16px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-body p { font-size: 12px; color: #999; margin: 8px 0 12px; min-height: 18px; }
.product-footer { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.price { color: #e74c3c; font-weight: 700; font-size: 18px; }
.sales { color: #999; font-size: 12px; }
.pager { justify-content: center; margin-top: 24px; }
@media (max-width: 640px) {
  .page-head { flex-direction: column; }
  .sort-select { width: 100%; }
  .product-cover { height: 140px; }
}
</style>
