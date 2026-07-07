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
      <el-button :icon="Download" @click="exportCsv">导出</el-button>
    </div>

    <el-table :data="rows" size="small">
      <el-table-column prop="question" label="问题" min-width="220" />
      <el-table-column prop="answer" label="回答" min-width="260" show-overflow-tooltip />
      <el-table-column prop="qualityStatus" label="质量" width="100" />
      <el-table-column prop="reviewStatus" label="审核" width="100" />
      <el-table-column prop="reviewer" label="审核人" width="110" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="openReview(row)">审核</el-button>
        </template>
      </el-table-column>
    </el-table>

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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="success" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Download, Plus, Search } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const rows = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const filters = reactive({ reviewStatus: '', keyword: '' })
const form = reactive({ question: '', answer: '', source: 'manual', qualityStatus: 'good', reviewStatus: 'pending', reviewer: '', reviewRemark: '' })

onMounted(load)

async function load() {
  rows.value = await api.sampleCandidates(filters)
}

function openCreate() {
  editing.value = null
  Object.assign(form, { question: '', answer: '', source: 'manual', qualityStatus: 'good', reviewStatus: 'pending', reviewer: '', reviewRemark: '' })
  dialogVisible.value = true
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
