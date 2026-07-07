<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="名称/学名/别名" clearable @keyup.enter="handleSearch" style="width: 200px" />
        </el-form-item>
        <el-form-item label="发布状态">
          <el-select v-model="queryForm.isPublished" placeholder="全部" clearable style="width: 120px">
            <el-option label="已发布" :value="1" />
            <el-option label="未发布" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar">
      <span class="toolbar-title">百科管理</span>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增词条</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="name" label="名称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="scientificName" label="学名" width="140" show-overflow-tooltip />
      <el-table-column prop="family" label="科" width="100" align="center" />
      <el-table-column prop="genus" label="属" width="100" align="center" />
      <el-table-column prop="isPublished" label="发布" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.isPublished === 1"
            inline-prompt active-text="是" inactive-text="否"
            @change="() => handleTogglePublish(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="70" align="center" />
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper" background
      @current-change="fetchData" @size-change="fetchData"
    />

    <el-dialog
      v-model="dialogVisible" :title="isEdit ? '编辑词条' : '新增词条'"
      width="680px" :close-on-click-modal="false" destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="词条名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入名称" />
              <el-button type="warning" size="small" style="margin-left:8px" :loading="aiGenerating" @click="openAiDraft">AI 生成</el-button>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学名">
              <el-input v-model="formData.scientificName" placeholder="拉丁学名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="别名">
              <el-input v-model="formData.alias" placeholder="多个用逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="拼音">
              <el-input v-model="formData.pinyin" placeholder="如: hongfushi" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="科">
              <el-input v-model="formData.family" placeholder="如: 蔷薇科" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="属">
              <el-input v-model="formData.genus" placeholder="如: 苹果属" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发布">
              <el-switch v-model="formData.isPublished" :active-value="1" :inactive-value="0" />
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
          <el-col :span="12">
            <el-form-item label="标签">
              <el-input v-model="formData.tags" placeholder="逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="简介" prop="description">
              <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="简要描述" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="形态特征">
              <el-input v-model="formData.morphology" type="textarea" :rows="2" placeholder="形态特征描述" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="分布">
              <el-input v-model="formData.distribution" type="textarea" :rows="1" placeholder="分布地区" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="生长环境">
              <el-input v-model="formData.habitat" type="textarea" :rows="1" placeholder="生长环境" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="养护指南">
              <el-input v-model="formData.careGuide" type="textarea" :rows="2" placeholder="养护要点" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="价值说明">
              <el-input v-model="formData.valueDescription" type="textarea" :rows="2" placeholder="经济/生态/观赏价值" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiVisible" title="AI 生成百科词条" width="400px" :close-on-click-modal="false">
      <el-form><el-form-item label="植物名称"><el-input v-model="aiName" placeholder="如: 巨峰葡萄" /></el-form-item></el-form>
      <template #footer><el-button @click="aiVisible=false">取消</el-button><el-button type="primary" :loading="aiGenerating" @click="handleAiDraft">生成</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { resolveImageUrl } from '@/utils/format'
import {
  getEncyclopediaPage, getEncyclopediaDetail,
  saveEncyclopedia, updateEncyclopedia, deleteEncyclopedia,
  toggleEncyclopediaPublish,
  uploadAdminImage
} from '@/api/admin'

const queryForm = reactive({ keyword: '', isPublished: null, page: 1, size: 10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editId = ref(null)
const galleryImages = ref([])
const MAX_GALLERY_IMAGES = 5

const defaultForm = () => ({
  name: '', scientificName: '', alias: '', pinyin: '',
  family: '', genus: '', coverImage: '', images: '',
  tags: '', description: '', morphology: '', distribution: '',
  habitat: '', careGuide: '', valueDescription: '', isPublished: 0
})

const formData = reactive(defaultForm())
const formRules = { name: [{ required: true, message: '请输入词条名称', trigger: 'blur' }] }

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryForm }
    if (params.isPublished === null || params.isPublished === '') delete params.isPublished
    if (!params.keyword) delete params.keyword
    const res = await getEncyclopediaPage(params)
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch { /* ignore */ } finally { loading.value = false }
}

const handleSearch = () => { queryForm.page = 1; fetchData() }
const handleReset = () => {
  queryForm.keyword = ''; queryForm.isPublished = null; queryForm.page = 1; fetchData()
}

const handleAdd = () => {
  isEdit.value = false; editId.value = null
  Object.assign(formData, defaultForm())
  galleryImages.value = []
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true; editId.value = row.id
  galleryImages.value = []
  try {
    const res = await getEncyclopediaDetail(row.id)
    if (res.code === 200 && res.data) {
      Object.assign(formData, {
        name: res.data.name || '', scientificName: res.data.scientificName || '',
        alias: res.data.alias || '', pinyin: res.data.pinyin || '',
        family: res.data.family || '', genus: res.data.genus || '',
        coverImage: res.data.coverImage || '', images: res.data.images || '',
        tags: res.data.tags || '', description: res.data.description || '',
        morphology: res.data.morphology || '', distribution: res.data.distribution || '',
        habitat: res.data.habitat || '', careGuide: res.data.careGuide || '',
        valueDescription: res.data.valueDescription || '', isPublished: res.data.isPublished ?? 0
      })
      galleryImages.value = parseImages(res.data.images)
    }
  } catch { /* ignore */ }
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...formData, images: JSON.stringify(galleryImages.value) }
    isEdit.value
      ? await updateEncyclopedia(editId.value, payload)
      : await saveEncyclopedia(payload)
    ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
    dialogVisible.value = false
    fetchData()
  } catch { /* ignore */ } finally { saving.value = false }
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
  const res = await uploadAdminImage(file, 'encyclopedia_image')
  formData.coverImage = res.data.url
  ElMessage.success('图片上传成功')
  return false
}

const handleGalleryUpload = async (file) => {
  if (galleryImages.value.length >= MAX_GALLERY_IMAGES) {
    ElMessage.warning(`百科多图最多上传 ${MAX_GALLERY_IMAGES} 张`)
    return false
  }
  if (!validateImage(file)) return false
  const res = await uploadAdminImage(file, 'encyclopedia_image')
  galleryImages.value.push(res.data.url)
  if (!formData.coverImage) formData.coverImage = res.data.url
  ElMessage.success('图片上传成功')
  return false
}

const removeGalleryImage = (index) => {
  galleryImages.value.splice(index, 1)
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除词条「${row.name}」？`, '删除确认',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    await deleteEncyclopedia(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

const handleTogglePublish = async (row) => {
  try {
    await toggleEncyclopediaPublish(row.id)
    row.isPublished = row.isPublished === 1 ? 0 : 1
    ElMessage.success(row.isPublished ? '已发布' : '已下架')
  } catch { /* ignore */ }
}

const aiVisible = ref(false), aiName = ref(''), aiGenerating = ref(false)
const openAiDraft = () => { aiName.value = ''; aiVisible.value = true }
const handleAiDraft = async () => {
  if (!aiName.value.trim()) return ElMessage.warning('请输入植物名称')
  aiGenerating.value = true
  try {
    const res = await request({ url: '/admin/ai/encyclopedia/draft', method: 'post', data: { name: aiName.value } })
    if (res.code === 200 && res.data) {
      const d = res.data
      formData.name = d.name || aiName.value
      formData.scientificName = d.scientificName || ''; formData.alias = d.alias || ''
      formData.pinyin = d.pinyin || ''; formData.family = d.family || ''
      formData.genus = d.genus || ''; formData.description = d.description || ''
      formData.morphology = d.morphology || ''; formData.distribution = d.distribution || ''
      formData.habitat = d.habitat || ''; formData.careGuide = d.careGuide || ''
      formData.valueDescription = d.valueDescription || ''; formData.tags = d.tags || ''
      ElMessage.success('词条已生成，请检查修改后保存'); aiVisible.value = false
    } else { ElMessage.error(res.msg || '生成失败') }
  } catch { ElMessage.error('AI 服务暂不可用') } finally { aiGenerating.value = false }
}

onMounted(() => { fetchData() })
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
