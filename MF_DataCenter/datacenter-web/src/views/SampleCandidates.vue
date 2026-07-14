<template>
  <section class="panel">
    <div class="toolbar">
      <el-select v-model="filters.reviewStatus" clearable placeholder="审核状态" @change="load">
        <el-option label="待审核" value="pending" />
        <el-option label="通过" value="approved" />
        <el-option label="拒绝" value="rejected" />
      </el-select>
      <el-input v-model="filters.keyword" placeholder="关键词" clearable @keyup.enter="load" />
      <el-button :icon="Search" @click="load">查询</el-button>
      <el-button type="success" :icon="Plus" @click="openCreate">新增</el-button>
      <el-button type="success" :disabled="!selected.length" @click="batchApprove">批量通过</el-button>
      <el-button :icon="Download" @click="exportCsv">导出</el-button>
    </div>

    <el-table :data="rows" size="small" @selection-change="selected = $event">
      <el-table-column type="selection" width="44" />
      <el-table-column prop="question" label="问题" min-width="220" />
      <el-table-column prop="answer" label="回答" min-width="260" show-overflow-tooltip />
      <el-table-column prop="qualityStatus" label="质量" width="100" />
      <el-table-column prop="reviewStatus" label="审核" width="100" />
      <el-table-column prop="reviewer" label="审核人" width="110" />
      <el-table-column prop="recommendedForKnowledge" label="推荐入库" width="110"><template #default="{ row }"><el-tag :type="row.recommendedForKnowledge ? 'success' : 'info'" effect="plain">{{ row.recommendedForKnowledge ? '推荐' : '未推荐' }}</el-tag></template></el-table-column>
      <el-table-column prop="conversationId" label="关联咨询" width="100" />
      <el-table-column prop="updateTime" label="最近更新" width="180" />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="openReview(row)">审核</el-button>
          <el-tooltip :disabled="canCreateKnowledge(row)" content="需先审核通过并勾选推荐入库" placement="top">
            <span><el-button link type="primary" :disabled="!canCreateKnowledge(row)" @click="openKnowledgeDraft(row)">创建知识草稿</el-button></span>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <TablePager v-bind="page" @change="changePage" />

    <el-dialog v-model="dialogVisible" :title="editing ? '审核样本候选' : '新增样本候选'" width="700px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="问题"><el-input v-model="form.question" type="textarea" :disabled="editing" /></el-form-item>
        <el-form-item label="回答"><el-input v-model="form.answer" type="textarea" :disabled="editing" /></el-form-item>
        <div class="form-row">
          <el-form-item label="质量状态"><el-input v-model="form.qualityStatus" :disabled="editing" /></el-form-item>
          <el-form-item label="审核状态">
            <el-select v-model="form.reviewStatus">
              <el-option label="待审核" value="pending" />
              <el-option label="通过" value="approved" />
              <el-option label="拒绝" value="rejected" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="来源"><el-input v-model="form.source" :disabled="editing" /></el-form-item>
          <el-form-item label="审核人"><el-input v-model="form.reviewer" /></el-form-item>
        </div>
        <el-form-item label="审核备注"><el-input v-model="form.reviewRemark" type="textarea" /></el-form-item>
        <el-form-item label="推荐入库"><el-switch v-model="form.recommendedForKnowledge" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="success" @click="submit">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="knowledgeDialog" title="从优质回答创建知识草稿" width="560px">
      <el-form :model="knowledgeForm" label-width="84px">
        <el-form-item label="样本问题"><el-input :model-value="knowledgeSample?.question" disabled /></el-form-item>
        <el-form-item label="内容类型"><el-select v-model="knowledgeForm.contentType"><el-option label="FAQ" value="faq" /><el-option label="百科" value="encyclopedia" /><el-option label="文章" value="article" /></el-select></el-form-item>
        <el-form-item label="草稿标题"><el-input v-model="knowledgeForm.title" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="knowledgeForm.tags" /></el-form-item>
        <el-form-item label="风险等级"><el-select v-model="knowledgeForm.riskLevel"><el-option label="低风险" value="low" /><el-option label="中风险" value="medium" /><el-option label="高风险（仅人工复核）" value="high" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="knowledgeDialog = false">取消</el-button><el-button type="primary" @click="createKnowledgeDraft">创建草稿</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Download, Plus, Search } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import TablePager from '../components/TablePager.vue'

const rows = ref([])
const selected = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const router = useRouter()
const knowledgeDialog = ref(false)
const knowledgeSample = ref(null)
const filters = reactive({ reviewStatus: '', keyword: '' })
const page = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const form = reactive({ question: '', answer: '', source: 'manual', qualityStatus: 'good', reviewStatus: 'pending', reviewer: '', reviewRemark: '', recommendedForKnowledge: false })
const knowledgeForm = reactive({ contentType: 'encyclopedia', title: '', tags: '', riskLevel: 'low' })

onMounted(load)

async function load() {
  const result = await api.sampleCandidatePage({ ...filters, pageNo: page.pageNo, pageSize: page.pageSize })
  rows.value = result.records
  Object.assign(page, result)
}

async function changePage (next) { Object.assign(page, next); await load() }

function openCreate() {
  editing.value = null
  Object.assign(form, { question: '', answer: '', source: 'manual', qualityStatus: 'good', reviewStatus: 'pending', reviewer: '', reviewRemark: '', recommendedForKnowledge: false })
  dialogVisible.value = true
}

async function batchApprove() {
  await Promise.all(selected.value.map(row => api.reviewSampleCandidate(row.id, { reviewStatus: 'approved', reviewer: row.reviewer || 'ops', reviewRemark: row.reviewRemark, recommendedForKnowledge: true })))
  ElMessage.success('已批量通过并标记推荐入库')
  await load()
}

function openReview(row) {
  editing.value = row
  Object.assign(form, row)
  dialogVisible.value = true
}

async function submit() {
  if (editing.value) {
    await api.reviewSampleCandidate(editing.value.id, form)
  } else {
    await api.createSampleCandidate(form)
  }
  ElMessage.success('已保存')
  dialogVisible.value = false
  await load()
}

function canCreateKnowledge (row) { return row.reviewStatus === 'approved' && row.recommendedForKnowledge === true }
function openKnowledgeDraft (row) { knowledgeSample.value = row; Object.assign(knowledgeForm, { contentType: 'encyclopedia', title: row.question, tags: '', riskLevel: 'low' }); knowledgeDialog.value = true }
async function createKnowledgeDraft () {
  const result = await api.createKnowledgeDraftFromSample(knowledgeSample.value.id, knowledgeForm)
  knowledgeDialog.value = false
  ElMessage.success(result.created ? '知识草稿已创建' : '该样本已创建过知识草稿，已为你打开')
  await router.push({ path: '/knowledge-workbench', query: { gapId: result.gapId, candidateId: result.candidateId } })
}

function exportCsv() {
  if (!rows.value.length) {
    ElMessage.info('暂无可导出的样本')
    return
  }
  const headers = ['id', 'question', 'answer', 'source', 'qualityStatus', 'reviewStatus', 'reviewer', 'reviewRemark']
  const lines = [
    headers.join(','),
    ...rows.value.map((row) => headers.map((key) => csvCell(row[key])).join(','))
  ]
  const blob = new Blob([`\uFEFF${lines.join('\n')}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `mf-sample-candidates-${Date.now()}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

function csvCell(value) {
  const text = value == null ? '' : String(value)
  return `"${text.replaceAll('"', '""')}"`
}
</script>
