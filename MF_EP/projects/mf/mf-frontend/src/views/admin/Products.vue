<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="商品名称">
          <el-input v-model="queryForm.name" placeholder="请输入名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="商品类型">
          <el-select v-model="queryForm.productType" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="树苗" value="tree" />
            <el-option label="化肥" value="fertilizer" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar">
      <span class="toolbar-title">商品管理</span>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增商品</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="商品名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="productType" label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.productType === 'tree' ? 'success' : 'warning'">
            {{ row.productType === 'tree' ? '树苗' : '化肥' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="100" align="center">
        <template #default="{ row }">
          {{ row.price != null ? formatCurrency(row.price) : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80" align="center" />
      <el-table-column prop="salesCount" label="销量" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            inline-prompt
            active-text="上架"
            inactive-text="下架"
            @change="(val) => handleToggleStatus(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="isRecommend" label="推荐" width="70" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.isRecommend === 1 ? 'danger' : 'info'">
            {{ row.isRecommend === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="success" link size="small" @click="handleToggleRecommend(row)">推荐</el-button>
          <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="queryForm.page"
      v-model:page-size="queryForm.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @current-change="fetchData"
      @size-change="fetchData"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="680px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品类型" prop="productType">
              <el-select v-model="formData.productType" placeholder="请选择" style="width: 100%">
                <el-option label="树苗" value="tree" />
                <el-option label="化肥" value="fertilizer" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="formData.categoryId" placeholder="请选择分类" clearable style="width: 100%">
                <el-option
                  v-for="cat in categoryOptions"
                  :key="cat.id"
                  :label="cat.name"
                  :value="cat.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="formData.brand" placeholder="请输入品牌" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价" prop="price">
              <el-input-number v-model="formData.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价">
              <el-input-number v-model="formData.originalPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="运费">
              <el-input-number v-model="formData.freight" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="formData.stock" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="formData.unit" placeholder="请选择" clearable style="width: 100%">
                <el-option label="棵" value="棵" />
                <el-option label="千克(kg)" value="kg" />
                <el-option label="袋" value="bag" />
                <el-option label="吨(t)" value="t" />
                <el-option label="升(L)" value="L" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-radio-group v-model="formData.status">
                <el-radio :value="1">上架</el-radio>
                <el-radio :value="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面图">
              <div class="image-upload">
                <el-upload :show-file-list="false" :before-upload="handleCoverUpload" accept="image/*">
                  <el-button>上传图片</el-button>
                </el-upload>
                <el-input v-model="formData.coverImage" placeholder="图片URL" />
                <el-image v-if="formData.coverImage" :src="imageUrl(formData.coverImage)" fit="cover" class="cover-preview" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="多图">
              <div class="gallery-upload">
                <el-upload multiple :show-file-list="false" :before-upload="handleGalleryUpload" accept="image/*">
                  <el-button>上传多图</el-button>
                </el-upload>
                <div class="gallery-list">
                  <div v-for="(url, index) in galleryImages" :key="url" class="gallery-item">
                    <el-image :src="imageUrl(url)" fit="cover" class="gallery-preview" />
                    <el-button link type="danger" size="small" @click="removeGalleryImage(index)">删除</el-button>
                  </div>
                </div>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="推荐">
              <el-switch v-model="formData.isRecommend" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="新品">
              <el-switch v-model="formData.isNew" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品描述">
              <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getProductPage, getProductDetail,
  saveProduct, updateProduct, deleteProduct,
  toggleProductStatus, toggleProductRecommend,
  uploadAdminImage
} from '@/api/admin'
import { getCategoryPage } from '@/api/admin'
import { formatCurrency, resolveImageUrl } from '@/utils/format'

const categoryOptions = ref([])

const fetchCategories = async () => {
  try {
    const res = await getCategoryPage({ page: 1, size: 100 })
    if (res.code === 200 && res.data) {
      categoryOptions.value = res.data.records || []
    }
  } catch { /* ignore */ }
}

const queryForm = reactive({ name: '', productType: '', status: null, page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editId = ref(null)
const galleryImages = ref([])
const MAX_GALLERY_IMAGES = 6

const defaultForm = () => ({
  name: '',
  productType: '',
  categoryId: null,
  brand: '',
  coverImage: '',
  images: '',
  price: 0,
  originalPrice: undefined,
  freight: 0,
  stock: 0,
  unit: '',
  status: 1,
  isRecommend: 0,
  isNew: 0,
  description: ''
})

const formData = reactive(defaultForm())

const formRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  productType: [{ required: true, message: '请选择商品类型', trigger: 'change' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryForm }
    if (params.status === null || params.status === '') delete params.status
    if (!params.name) delete params.name
    if (!params.productType) delete params.productType
    const res = await getProductPage(params)
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch { /* ignore */ } finally { loading.value = false }
}

const handleSearch = () => { queryForm.page = 1; fetchData() }

const handleReset = () => {
  queryForm.name = ''
  queryForm.productType = ''
  queryForm.status = null
  queryForm.page = 1
  fetchData()
}

const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  Object.assign(formData, defaultForm())
  galleryImages.value = []
  fetchCategories()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  editId.value = row.id
  galleryImages.value = []
  fetchCategories()
  try {
    const res = await getProductDetail(row.id)
    if (res.code === 200 && res.data) {
      const p = res.data.product
      const d = res.data.detail
      Object.assign(formData, {
        name: p.name || '',
        productType: p.productType || '',
        categoryId: p.categoryId,
        brand: p.brand || '',
        coverImage: p.coverImage || '',
        images: p.images || '',
        price: p.price ?? 0,
        originalPrice: p.originalPrice,
        freight: p.freight ?? 0,
        stock: p.stock ?? 0,
        unit: p.unit || '',
        status: p.status ?? 1,
        isRecommend: p.isRecommend ?? 0,
        isNew: p.isNew ?? 0,
        description: p.description || ''
      })
      galleryImages.value = parseImages(p.images)
    }
  } catch { /* ignore */ }
  dialogVisible.value = true
}

const parseImages = (value) => {
  if (!value) return []
  if (Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return value.split(',').map(item => item.trim()).filter(Boolean)
  }
}

const imageUrl = (url) => {
  return resolveImageUrl(url)
}

const validateImage = (file) => {
  if (!file.type?.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('图片不能超过5MB')
    return false
  }
  return true
}

const handleCoverUpload = async (file) => {
  if (!validateImage(file)) return false
  const res = await uploadAdminImage(file, 'product_image')
  formData.coverImage = res.data.url
  ElMessage.success('图片上传成功')
  return false
}

const handleGalleryUpload = async (file) => {
  if (galleryImages.value.length >= MAX_GALLERY_IMAGES) {
    ElMessage.warning(`商品轮播图最多上传 ${MAX_GALLERY_IMAGES} 张`)
    return false
  }
  if (!validateImage(file)) return false
  const res = await uploadAdminImage(file, 'product_image')
  galleryImages.value.push(res.data.url)
  if (!formData.coverImage) formData.coverImage = res.data.url
  ElMessage.success('图片上传成功')
  return false
}

const removeGalleryImage = (index) => {
  galleryImages.value.splice(index, 1)
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...formData, images: JSON.stringify(galleryImages.value) }
    if (isEdit.value) {
      await updateProduct(editId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await saveProduct(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch { /* ignore */ } finally { saving.value = false }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确认删除商品「${row.name}」？`,
    '删除确认',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

const handleToggleStatus = async (row, val) => {
  try {
    await toggleProductStatus(row.id, val ? 1 : 0)
    row.status = val ? 1 : 0
    ElMessage.success(val ? '已上架' : '已下架')
  } catch { /* ignore */ }
}

const handleToggleRecommend = async (row) => {
  try {
    await toggleProductRecommend(row.id)
    row.isRecommend = row.isRecommend === 1 ? 0 : 1
    ElMessage.success('切换成功')
  } catch { /* ignore */ }
}

onMounted(() => {
  fetchData()
  fetchCategories()
})
</script>

<style scoped>
.image-upload {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
  width: 100%;
}

.cover-preview {
  width: 64px;
  height: 64px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.gallery-upload {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.gallery-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.gallery-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.gallery-preview {
  width: 72px;
  height: 72px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
</style>
