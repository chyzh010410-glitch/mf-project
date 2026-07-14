<template>
  <section class="panel">
    <div class="toolbar">
      <el-select v-model="filters.status" clearable placeholder="状态" @change="load">
        <el-option label="待处理" value="pending" />
        <el-option label="处理中" value="processing" />
        <el-option label="已解决" value="resolved" />
        <el-option label="忽略" value="ignored" />
      </el-select>
      <el-input v-model="filters.keyword" placeholder="关键词" clearable @keyup.enter="load" />
      <el-button :icon="Search" @click="load">查询</el-button>
      <el-button type="success" :icon="Plus" @click="openCreate">新增</el-button>
    </div>

    <el-table :data="rows" size="small">
      <el-table-column prop="question" label="问题" min-width="220" />
      <el-table-column prop="reason" label="原因" min-width="160" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="priority" label="优先级" width="100" />
      <el-table-column prop="owner" label="负责人" width="120" />
      <el-table-column prop="dueTime" label="截止时间" width="180" />
      <el-table-column prop="conversationId" label="关联咨询" width="100" />
      <el-table-column prop="updateTime" label="最近更新" width="180" />
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="openEdit(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePager v-bind="page" @change="changePage" />

    <el-dialog v-model="dialogVisible" :title="editing ? '处理未解决问题' : '新增未解决问题'" width="640px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="问题"><el-input v-model="form.question" type="textarea" :disabled="editing" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="form.reason" :disabled="editing" /></el-form-item>
        <div class="form-row">
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="待处理" value="pending" />
              <el-option label="处理中" value="processing" />
              <el-option label="已解决" value="resolved" />
              <el-option label="忽略" value="ignored" />
            </el-select>
          </el-form-item>
          <el-form-item label="负责人"><el-input v-model="form.owner" /></el-form-item>
        </div>
        <div class="form-row"><el-form-item label="优先级"><el-select v-model="form.priority"><el-option label="低" value="low" /><el-option label="普通" value="normal" /><el-option label="高" value="high" /><el-option label="紧急" value="urgent" /></el-select></el-form-item><el-form-item label="截止时间"><el-date-picker v-model="form.dueTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item></div>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
        <el-form-item label="知识动作"><el-input v-model="form.knowledgeAction" placeholder="例如：补充百科或优化 Agent 提示词" /></el-form-item>
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
import { Plus, Search } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import TablePager from '../components/TablePager.vue'

const rows = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const filters = reactive({ status: '', keyword: '' })
const page = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const form = reactive({ question: '', reason: '', status: 'pending', priority: 'normal', dueTime: null, owner: '', remark: '', knowledgeAction: '' })

onMounted(load)

async function load() {
  const result = await api.unresolvedQuestionPage({ ...filters, pageNo: page.pageNo, pageSize: page.pageSize })
  rows.value = result.records
  Object.assign(page, result)
}

async function changePage (next) { Object.assign(page, next); await load() }

function openCreate() {
  editing.value = null
  Object.assign(form, { question: '', reason: '', status: 'pending', priority: 'normal', dueTime: null, owner: '', remark: '', knowledgeAction: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, row)
  dialogVisible.value = true
}

async function submit() {
  if (editing.value) {
    await api.updateUnresolvedQuestion(editing.value.id, form)
  } else {
    await api.createUnresolvedQuestion(form)
  }
  ElMessage.success('已保存')
  dialogVisible.value = false
  await load()
}
</script>
