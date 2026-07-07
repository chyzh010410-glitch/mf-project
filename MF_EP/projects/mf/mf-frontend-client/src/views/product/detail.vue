<template>
  <div class="page-container">
    <div v-if="product" v-loading="loading">
      <el-row :gutter="40">
        <el-col :xs="24" :md="12">
          <el-carousel v-if="carouselImages.length" class="product-carousel" height="420px" indicator-position="outside">
            <el-carousel-item v-for="image in carouselImages" :key="image">
              <el-image :src="resolveImageUrl(image)" fit="cover" class="carousel-image" />
            </el-carousel-item>
          </el-carousel>
          <div v-else class="product-image">
            <span>{{ typeLabel(product.productType) }}</span>
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="product-info">
            <el-tag :type="product.productType === 'fertilizer' ? 'warning' : 'success'">
              {{ typeLabel(product.productType) }}
            </el-tag>
            <h1>{{ product.name }}</h1>
            <p class="brand">{{ product.brand || '平台精选' }}</p>

            <div class="price-box">
              <span class="price">{{ formatCurrency(product.price) }}</span>
              <span v-if="product.originalPrice" class="original-price">{{ formatCurrency(product.originalPrice) }}</span>
            </div>

            <div class="meta-line">
              <span>库存：{{ product.stock || 0 }} {{ product.unit || '件' }}</span>
              <span>销量：{{ product.salesCount || 0 }}</span>
              <span>运费：{{ formatFreight(product.freight) }}</span>
            </div>

            <div class="buy-line">
              <template v-if="product.stock > 0">
                <el-input-number v-model="quantity" :min="1" :max="product.stock" />
                <span class="unit">{{ product.unit || '件' }}</span>
              </template>
              <el-tag v-else type="danger" size="large">已售罄</el-tag>
            </div>

            <div class="actions">
              <el-button type="primary" size="large" :disabled="product.stock === 0" @click="handleBuy">
                立即购买
              </el-button>
              <el-button size="large" :loading="adding" :disabled="product.stock === 0" @click="handleAddCart">
                加入购物车
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-descriptions title="商品详情" :column="2" border class="detail-desc">
        <el-descriptions-item label="类型">{{ typeLabel(product.productType) }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ product.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ product.unit || '-' }}</el-descriptions-item>
        <el-descriptions-item label="运费">{{ formatFreight(product.freight) }}</el-descriptions-item>
      </el-descriptions>

      <div class="description">
        <h3>商品介绍</h3>
        <div v-if="product.description" v-html="product.description"></div>
        <el-empty v-else description="暂无商品介绍" />
      </div>
    </div>

    <el-empty v-else-if="!loading" description="商品不存在或已下架" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addToCart } from '@/api/cart'
import { getProductDetail } from '@/api/product'
import { formatCurrency, parseImageList, resolveImageUrl } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const quantity = ref(1)
const loading = ref(false)
const adding = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getProductDetail(route.params.id)
    if (res.code === 200) product.value = res.data
  } catch {
    // request interceptor already shows the message
  } finally {
    loading.value = false
  }
}

const handleAddCart = async () => {
  adding.value = true
  try {
    await addToCart({ productId: product.value.id, quantity: quantity.value })
    ElMessage.success('已加入购物车')
  } catch {
    // request interceptor already shows the message
  } finally {
    adding.value = false
  }
}

const handleBuy = () => {
  const items = [{
    productId: product.value.id,
    productName: product.value.name,
    productImage: product.value.coverImage,
    productType: product.value.productType,
    price: product.value.price,
    freight: product.value.freight,
    stock: product.value.stock,
    unit: product.value.unit,
    quantity: quantity.value
  }]
  router.push({ name: 'Checkout', state: { buyNowItems: items } })
}

const carouselImages = computed(() => {
  if (!product.value) return []
  const images = parseImageList(product.value.images)
  if (images.length) return images
  return product.value.coverImage ? [product.value.coverImage] : []
})

const typeLabel = (type) => type === 'fertilizer' ? '肥料' : '树苗'
const formatFreight = (freight) => Number(freight) > 0 ? formatCurrency(freight) : '免运费'

onMounted(fetchData)
</script>

<style scoped>
.product-image { height: 420px; border-radius: 16px; background: linear-gradient(135deg, #e8f5e9, #f7efe0); background-size: cover; background-position: center; display: flex; align-items: center; justify-content: center; color: var(--color-primary); font-size: 42px; font-weight: 700; }
.product-carousel { border-radius: 16px; overflow: hidden; background: #f5f7f6; }
.carousel-image { width: 100%; height: 420px; display: block; }
.product-info { background: #fff; border: 1px solid #ebeef5; border-radius: 16px; padding: 24px; min-height: 420px; }
.product-info h1 { margin: 14px 0 8px; color: #25352b; font-size: 30px; }
.brand { color: #8b929b; margin: 0 0 18px; }
.price-box { background: #fff8f0; border-radius: 12px; padding: 18px; margin-bottom: 18px; }
.price { font-size: 32px; color: #e74c3c; font-weight: 800; }
.original-price { color: #999; text-decoration: line-through; margin-left: 12px; }
.meta-line { display: flex; flex-wrap: wrap; gap: 14px; color: #66706a; font-size: 14px; margin-bottom: 20px; }
.buy-line { display: flex; align-items: center; gap: 10px; margin-bottom: 22px; }
.unit { color: #8b929b; }
.actions { display: flex; gap: 12px; }
.detail-desc { margin-top: 30px; }
.description { background: #fff; border-radius: 14px; border: 1px solid #ebeef5; padding: 22px; margin-top: 22px; color: #4b5563; line-height: 1.8; }
.description h3 { margin: 0 0 14px; color: #25352b; }
@media (max-width: 768px) {
  .product-image { height: 300px; margin-bottom: 18px; }
  .product-info h1 { font-size: 24px; }
  .actions { flex-direction: column; }
}
</style>
