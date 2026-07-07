<template>
  <div class="page-container">
    <h2 class="section-title">{{ isBuyNow ? '直接购买' : '确认订单' }}</h2>
    <el-row :gutter="24">
      <el-col :span="16">
        <!-- 收货地址 -->
        <div class="checkout-section">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
            <h3>收货地址</h3>
            <el-button type="primary" size="small" @click="showAddrDialog = true">＋ 新增地址</el-button>
          </div>
          <div v-if="addresses.length" style="display:flex;flex-wrap:wrap;gap:12px">
            <div
              v-for="addr in addresses" :key="addr.id"
              class="address-card"
              :class="{ selected: selectedAddress?.id === addr.id }"
              @click="selectedAddress = addr"
            >
              <div style="display:flex;justify-content:space-between;align-items:center">
                <div>
                  <b>{{ addr.receiverName }}</b>
                  <span style="margin-left:12px;color:#666">{{ addr.receiverPhone }}</span>
                  <el-tag v-if="Number(addr.isDefault) === 1" size="small" type="success" style="margin-left:6px">默认</el-tag>
                </div>
                <el-button v-if="Number(addr.isDefault) !== 1" link type="primary" size="small" @click.stop="setDefault(addr)">设为默认</el-button>
              </div>
              <p style="color:#666;margin-top:6px;font-size:13px">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</p>
            </div>
          </div>
          <div v-else class="address-empty">
            <p style="color:#999;margin-bottom:12px">暂无收货地址，请先添加</p>
            <el-button type="primary" @click="showAddrDialog = true">添加地址</el-button>
          </div>
        </div>

        <!-- 商品清单 -->
        <div class="checkout-section">
          <h3>商品清单</h3>
          <div v-for="item in cartItems" :key="item.id" class="checkout-item">
            <span style="flex:1">{{ item.productName }}</span>
            <el-input-number
              v-model="item.quantity"
              :min="1" :max="999" size="small"
              style="margin-right:16px;width:110px"
              @change="() => { /* quantity adjusted locally */ }"
            />
            <span style="font-weight:600;color:#e74c3c">{{ formatCurrency(Number(item.price) * Number(item.quantity)) }}</span>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="checkout-section summary-card">
          <h3>订单汇总</h3>
          <div class="summary-row"><span>商品数量</span><span>{{ totalCount }} 件</span></div>
          <div class="summary-row"><span>商品总额</span><span>{{ productAmountText }}</span></div>
          <div class="summary-row"><span>运费</span><span>{{ freightAmountText }}</span></div>
          <el-divider />
          <div class="payment-method">
            <div class="payment-title">支付方式</div>
            <el-radio-group v-model="paymentMethod" class="payment-options">
              <el-radio-button label="wechat">微信支付</el-radio-button>
              <el-radio-button label="alipay">支付宝</el-radio-button>
            </el-radio-group>
          </div>
          <div class="summary-row summary-total"><span>应付金额</span><span style="color:#e74c3c;font-size:22px;font-weight:700">{{ payAmountText }}</span></div>
          <el-button
            type="primary" size="large"
            style="width:100%;margin-top:20px;height:48px;font-size:16px"
            :loading="submitting" :disabled="!selectedAddress || cartItems.length === 0"
            @click="handleSubmit"
          >立即支付</el-button>
        </div>
      </el-col>
    </el-row>

    <!-- 新增地址弹窗 -->
    <el-dialog v-model="showAddrDialog" title="新增收货地址" width="480px" destroy-on-close>
      <el-form ref="addrFormRef" :model="addrForm" :rules="addrRules" label-width="80px">
        <el-form-item label="收件人" prop="receiverName">
          <el-input v-model="addrForm.receiverName" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="addrForm.receiverPhone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="地区" prop="region">
          <el-cascader
            v-model="addrForm.region"
            :options="regionOptions"
            placeholder="省/市/区"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="addrForm.detail" placeholder="街道/门牌号" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addrForm.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddrDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingAddr" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCart } from '@/api/cart'
import { createOrder } from '@/api/order'
import { getAddresses, addAddress } from '@/api/address'
import { setDefaultAddress } from '@/api/address'
import { formatCurrency } from '@/utils/format'

const router = useRouter()
const isBuyNow = ref(false)
const cartItems = ref([])
const addresses = ref([])
const selectedAddress = ref(null)
const submitting = ref(false)
const showAddrDialog = ref(false)
const savingAddr = ref(false)
const addrFormRef = ref(null)
const paymentMethod = ref('wechat')

const addrForm = reactive({
  receiverName: '', receiverPhone: '', region: [], detail: '', isDefault: false
})
const addrRules = {
  receiverName: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  region: [{ required: true, message: '请选择地区', trigger: 'change' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

// Simplified region data
const regionOptions = [
  { value: '广东省', label: '广东省', children: [
    { value: '广州市', label: '广州市', children: [{ value: '天河区', label: '天河区' }, { value: '白云区', label: '白云区' }, { value: '海珠区', label: '海珠区' }] },
    { value: '深圳市', label: '深圳市', children: [{ value: '南山区', label: '南山区' }, { value: '福田区', label: '福田区' }] },
    { value: '东莞市', label: '东莞市', children: [{ value: '南城区', label: '南城区' }] }
  ]},
  { value: '浙江省', label: '浙江省', children: [
    { value: '杭州市', label: '杭州市', children: [{ value: '西湖区', label: '西湖区' }, { value: '余杭区', label: '余杭区' }] },
    { value: '宁波市', label: '宁波市', children: [{ value: '海曙区', label: '海曙区' }] }
  ]}
]

const totalCount = computed(() => cartItems.value.reduce((s, i) => s + Number(i.quantity), 0))
const productAmount = computed(() => cartItems.value.reduce((s, i) => s + Number(i.price) * Number(i.quantity), 0))
const freightAmount = computed(() => cartItems.value.reduce((s, i) => s + Number(i.freight || 0), 0))
const productAmountText = computed(() => formatCurrency(productAmount.value))
const freightAmountText = computed(() => freightAmount.value > 0 ? formatCurrency(freightAmount.value) : '免运费')
const payAmountText = computed(() => formatCurrency(productAmount.value + freightAmount.value))

onMounted(async () => {
  // 立即购买模式：直接用路由传过来的商品，不加载购物车
  const buyNowItems = history.state?.buyNowItems
  if (buyNowItems && buyNowItems.length > 0) {
    isBuyNow.value = true
    cartItems.value = buyNowItems.map(i => ({
      ...i,
      quantity: Number(i.quantity) || 1,
      price: Number(i.price),
      freight: Number(i.freight || 0),
      selected: 1,
      subtotal: Number(i.price) * Number(i.quantity)
    }))
  } else {
    try {
      const cartRes = await getCart()
      if (cartRes.code === 200 && cartRes.data) {
        cartItems.value = (cartRes.data.items || [])
          .filter(i => Number(i.selected) === 1)
          .map(i => ({ ...i, quantity: Number(i.quantity) || 1, price: Number(i.price), freight: Number(i.freight || 0) }))
      }
    } catch {}
  }
  try {
    const addrRes = await getAddresses()
    if (addrRes.code === 200 && addrRes.data) {
      addresses.value = addrRes.data
      selectedAddress.value = addrRes.data.find(a => Number(a.isDefault) === 1) || addrRes.data[0] || null
    }
  } catch {}
})

const setDefault = async (addr) => {
  try {
    const res = await setDefaultAddress(addr.id)
    if (res.code === 200) {
      addresses.value.forEach(a => a.isDefault = (a.id === addr.id ? 1 : 0))
      ElMessage.success('已设为默认地址')
    }
  } catch {}
}

const saveAddress = async () => {
  const valid = await addrFormRef.value.validate().catch(() => false)
  if (!valid) return
  savingAddr.value = true
  try {
    const data = {
      receiverName: addrForm.receiverName,
      receiverPhone: addrForm.receiverPhone,
      province: addrForm.region[0] || '',
      city: addrForm.region[1] || '',
      district: addrForm.region[2] || '',
      detail: addrForm.detail,
      isDefault: addrForm.isDefault ? 1 : 0
    }
    const res = await addAddress(data)
    if (res.code === 200) {
      ElMessage.success('地址已保存')
      showAddrDialog.value = false
      // Reload addresses
      const addrRes = await getAddresses()
      if (addrRes.code === 200 && addrRes.data) {
        addresses.value = addrRes.data
        if (!selectedAddress.value) selectedAddress.value = addrRes.data[0] || null
      }
    }
  } catch {} finally { savingAddr.value = false }
}

const handleSubmit = async () => {
  if (!selectedAddress.value) { ElMessage.warning('请选择收货地址'); return }
  if (cartItems.value.length === 0) { ElMessage.warning('没有商品'); return }
  submitting.value = true
  try {
    const items = cartItems.value.map(i => ({
      productId: i.productId,
      quantity: Number(i.quantity)
    }))
    const res = await createOrder({ addressId: selectedAddress.value.id, items })
    if (res.code === 200 && res.data) {
      router.push({ name: 'Cashier', params: { id: res.data.orderId }, query: { method: paymentMethod.value } })
    }
  } catch {} finally { submitting.value = false }
}
</script>

<style scoped>
.checkout-section { background: var(--color-white); border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.checkout-section h3 { font-size: 16px; font-weight: 600; margin: 0; }
.address-card { border: 2px solid var(--color-border); border-radius: 6px; padding: 12px; cursor: pointer; min-width: 240px; transition: all .2s; }
.address-card:hover { border-color: var(--color-primary-light); }
.address-card.selected { border-color: var(--color-primary); background: var(--color-primary-bg); }
.address-empty { padding: 20px; text-align: center; }
.checkout-item { display: flex; align-items: center; padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.summary-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; color: #666; }
.summary-total { font-size: 16px; font-weight: 600; color: var(--color-text); }
.payment-method { margin-bottom: 18px; }
.payment-title { margin-bottom: 10px; font-weight: 600; color: var(--color-text); }
.payment-options { width: 100%; }
.payment-options :deep(.el-radio-button) { flex: 1; }
.payment-options :deep(.el-radio-button__inner) { width: 100%; }
</style>
