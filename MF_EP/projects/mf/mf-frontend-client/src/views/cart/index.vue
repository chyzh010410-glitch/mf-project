<template>
  <div class="page-container">
    <h2 class="section-title">购物车</h2>
    <el-table
      v-if="cartItems.length"
      ref="tableRef"
      :data="cartItems"
      v-loading="loading"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column label="商品" min-width="280">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:12px;cursor:pointer" @click="$router.push('/product/'+row.productId)">
            <div style="width:80px;height:60px;background:#e8f5e9;display:flex;align-items:center;justify-content:center;border-radius:4px;flex-shrink:0">
              <span style="font-size:28px">{{ row.productName.includes('肥') ? '🧪' : '🌳' }}</span>
            </div>
            <div><p style="font-weight:500">{{ row.productName }}</p>
              <p style="font-size:12px;color:#999">库存 {{ row.stock }}</p></div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="单价" width="120"><template #default="{ row }">{{ formatCurrency(row.price) }}</template></el-table-column>
      <el-table-column label="数量" width="160">
        <template #default="{ row }">
          <el-input-number v-model="row.quantity" :min="1" :max="row.stock || 999" size="small" @change="(v) => updateQty(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="小计" width="120"><template #default="{ row }">{{ formatCurrency(Number(row.price) * (Number(row.quantity) || 1)) }}</template></el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ row, $index }"><el-button type="danger" link @click="removeItem(row, $index)">删除</el-button></template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="购物车是空的"><el-button type="primary" @click="$router.push('/home')">去逛逛</el-button></el-empty>
    <div v-if="cartItems.length" style="margin-top:20px;text-align:right;padding:16px;background:#fff;border-radius:8px">
      <span style="color:#999;margin-right:20px">已选 <b style="color:#2d8c4a">{{ selectedRows.length }}</b> 件</span>
      <span style="font-size:18px">合计：<b style="color:#e74c3c">{{ computedTotal }}</b></span>
      <el-button type="primary" size="large" :disabled="selectedRows.length === 0" style="margin-left:20px" @click="goCheckout">去结算</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCart, updateCartItem, removeCartItem } from '@/api/cart'
import { formatCurrency } from '@/utils/format'

const router = useRouter()
const cartItems = ref([])
const loading = ref(false)
const tableRef = ref(null)
const selectedRows = ref([])

const computedTotal = computed(() =>
  formatCurrency(selectedRows.value.reduce((s, i) => s + Number(i.price) * (Number(i.quantity) || 1), 0))
)

let selectionTimer = null

const onSelectionChange = (rows) => {
  selectedRows.value = rows
  clearTimeout(selectionTimer)
  selectionTimer = setTimeout(() => syncSelection(), 300)
}

const fetch = async () => {
  loading.value = true
  try {
    const res = await getCart()
    if (res.code === 200 && res.data) {
      cartItems.value = (res.data.items || []).map(item => ({
        ...item,
        quantity: Number(item.quantity) || 1,
        price: Number(item.price)
      }))
      // 恢复之前勾选的状态（selected = 1 的回显）
      await nextTick()
      cartItems.value.forEach((item, idx) => {
        if (Number(item.selected) === 1 && tableRef.value) {
          tableRef.value.toggleRowSelection(item, true)
        }
      })
    }
  } catch {} finally { loading.value = false }
}

const syncSelection = async () => {
  const selectedIds = new Set(selectedRows.value.map(r => r.id))
  for (const item of cartItems.value) {
    const shouldBeSelected = selectedIds.has(item.id) ? 1 : 0
    if (Number(item.selected) !== shouldBeSelected) {
      try {
        await updateCartItem(item.id, { selected: shouldBeSelected })
        item.selected = shouldBeSelected
      } catch {}
    }
  }
}

const updateQty = async (row, val) => {
  try { await updateCartItem(row.id, { quantity: val }) } catch { fetch() }
}

const removeItem = async (row, idx) => {
  try {
    await removeCartItem(row.id)
    cartItems.value.splice(idx, 1)
    ElMessage.success('已删除')
  } catch { fetch() }
}

const goCheckout = () => {
  if (selectedRows.value.length === 0) { ElMessage.warning('请选择要购买的商品'); return }
  // 先同步勾选状态再跳转
  syncSelection().then(() => router.push('/checkout'))
}

onMounted(fetch)
</script>
