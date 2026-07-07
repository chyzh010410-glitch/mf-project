<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="商品名称"><el-input v-model="queryForm.name" clearable style="width:200px" @keyup.enter="handleSearch" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryForm.productType" clearable placeholder="全部" style="width:130px">
            <el-option label="树苗" value="tree" />
            <el-option label="肥料" value="fertilizer" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部" style="width:110px">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="toolbar">
      <span class="toolbar-title">商品管理</span>
      <el-button type="primary" @click="openDialog()">新增商品</el-button>
    </div>
    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="name" label="商品名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="productType" label="类型" width="90" />
      <el-table-column prop="price" label="价格" width="90" />
      <el-table-column prop="stock" label="库存" width="90" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" background
      @current-change="fetchData" @size-change="fetchData" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.productType" style="width:100%">
            <el-option label="树苗" value="tree" />
            <el-option label="肥料" value="fertilizer" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="封面图">
          <div class="image-upload">
            <el-upload :show-file-list="false" :before-upload="handleImageUpload" accept="image/*">
              <el-button>上传图片</el-button>
            </el-upload>
            <el-input v-model="form.coverImage" placeholder="图片 URL" />
            <el-image v-if="form.coverImage" :src="imageUrl(form.coverImage)" fit="cover" class="cover-preview" />
          </div>
        </el-form-item>
        <el-form-item label="轮播图">
          <div class="gallery-upload">
            <el-upload multiple :show-file-list="false" :before-upload="handleGalleryUpload" accept="image/*">
              <el-button>上传轮播图</el-button>
            </el-upload>
            <div class="gallery-list">
              <div v-for="(url, index) in galleryImages" :key="url" class="gallery-item">
                <el-image :src="imageUrl(url)" fit="cover" class="gallery-preview" />
                <el-button link type="danger" size="small" @click="removeGalleryImage(index)">删除</el-button>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="原价"><el-input-number v-model="form.originalPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="运费"><el-input-number v-model="form.freight" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createProduct, deleteProduct, getProductDetail, getProductPage, updateProduct, uploadProductImage } from '@/api/merchant'

const queryForm = reactive({ name: '', productType: '', status: '', page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const form = reactive({})
const galleryImages = ref([])
const MAX_GALLERY_IMAGES = 6

const emptyForm = () => ({ id: null, name: '', productType: 'fertilizer', brand: '', coverImage: '', images: '', price: 0, originalPrice: null, stock: 0, unit: 'piece', freight: 0, status: 1, description: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryForm }
    if (!params.name) delete params.name
    if (!params.productType) delete params.productType
    if (params.status === '') delete params.status
    const res = await getProductPage(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { queryForm.page = 1; fetchData() }
const handleReset = () => { Object.assign(queryForm, { name: '', productType: '', status: '', page: 1, size: 10 }); fetchData() }

const openDialog = async (row) => {
  Object.assign(form, emptyForm())
  galleryImages.value = []
  if (row?.id) {
    const res = await getProductDetail(row.id)
    Object.assign(form, res.data?.product || row)
    galleryImages.value = parseImages(form.images)
  }
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
  if (!url) return ''
  return url.startsWith('http') ? url : `http://localhost:8080${url}`
}

const handleImageUpload = async (file) => {
  const isImage = file.type?.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 <= 5
  if (!isImage) {
    ElMessage.error('请选择图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片不能超过5MB')
    return false
  }
  const res = await uploadProductImage(file)
  form.coverImage = res.data.url
  ElMessage.success('图片上传成功')
  return false
}

const handleGalleryUpload = async (file) => {
  if (galleryImages.value.length >= MAX_GALLERY_IMAGES) {
    ElMessage.warning(`商品轮播图最多上传 ${MAX_GALLERY_IMAGES} 张`)
    return false
  }
  const isImage = file.type?.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 <= 5
  if (!isImage) {
    ElMessage.error('请选择图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片不能超过5MB')
    return false
  }
  const res = await uploadProductImage(file)
  galleryImages.value.push(res.data.url)
  if (!form.coverImage) {
    form.coverImage = res.data.url
  }
  ElMessage.success('轮播图上传成功')
  return false
}

const removeGalleryImage = (index) => {
  galleryImages.value.splice(index, 1)
}

const handleSave = async () => {
  const payload = { ...form }
  delete payload.id
  payload.images = JSON.stringify(galleryImages.value)
  if (form.id) {
    await updateProduct(form.id, payload)
  } else {
    await createProduct(payload)
  }
  ElMessage.success('已保存')
  dialogVisible.value = false
  fetchData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除商品「${row.name}」？`, '提示', { type: 'warning' })
  await deleteProduct(row.id)
  ElMessage.success('已删除')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.image-upload {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.cover-preview {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.gallery-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.gallery-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.gallery-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.gallery-preview {
  width: 88px;
  height: 88px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}
</style>
