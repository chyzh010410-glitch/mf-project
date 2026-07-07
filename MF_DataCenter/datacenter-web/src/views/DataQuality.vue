<template>
  <section class="page-grid">
    <div class="quality-summary">
      <div class="summary-item">
        <span>检查项</span>
        <strong>{{ summary.total }}</strong>
      </div>
      <div class="summary-item">
        <span>通过</span>
        <strong>{{ summary.passed }}</strong>
      </div>
      <div class="summary-item warning">
        <span>警告</span>
        <strong>{{ summary.warning }}</strong>
      </div>
      <div class="summary-item danger">
        <span>失败</span>
        <strong>{{ summary.failed }}</strong>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">质量检查结果</h2>
          <p class="muted">规则来自下方质量规则表，当前覆盖快照缺失、新鲜度、负值和今日日快照存在性。</p>
        </div>
        <el-button type="success" :icon="Play" @click="runChecks">立即检查</el-button>
      </div>
      <el-table :data="checks" size="small" stripe>
        <el-table-column prop="checkName" label="检查" width="160" />
        <el-table-column prop="metricCode" label="指标" width="170" />
        <el-table-column prop="status" label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row)" effect="plain">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="severity" label="等级" width="100" />
        <el-table-column prop="expectedValue" label="期望" width="150" />
        <el-table-column prop="actualValue" label="实际" width="120" />
        <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="checkTime" label="检查时间" min-width="180" />
      </el-table>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h2 class="panel-title">质量问题趋势</h2>
        <el-tag effect="plain">近 14 天</el-tag>
      </div>
      <el-table :data="issueTrend" size="small">
        <el-table-column prop="date" label="日期" />
        <el-table-column prop="open" label="打开" />
        <el-table-column prop="processing" label="处理中" />
        <el-table-column prop="resolved" label="已解决" />
        <el-table-column prop="ignored" label="已忽略" />
      </el-table>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h2 class="panel-title">质量规则</h2>
        <el-button type="primary" :icon="Plus" @click="openRuleCreate">新增规则</el-button>
      </div>
      <el-table :data="rules" size="small" stripe>
        <el-table-column prop="ruleCode" label="规则编码" width="190" />
        <el-table-column prop="ruleName" label="规则名称" width="160" />
        <el-table-column prop="checkType" label="检查类型" width="190" />
        <el-table-column prop="metricCode" label="限定指标" width="150" />
        <el-table-column prop="thresholdValue" label="阈值" width="110" />
        <el-table-column prop="severity" label="等级" width="100" />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Pencil" @click="openRuleEdit(row)">编辑</el-button>
            <el-button link :type="row.enabled ? 'warning' : 'success'" @click="toggleRule(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h2 class="panel-title">质量问题</h2>
        <div class="toolbar compact-toolbar">
          <el-select v-model="issueStatus" placeholder="状态" @change="loadIssues">
            <el-option label="全部" value="" />
            <el-option label="打开" value="open" />
            <el-option label="处理中" value="processing" />
            <el-option label="已解决" value="resolved" />
            <el-option label="已忽略" value="ignored" />
          </el-select>
        </div>
      </div>
      <el-table :data="issues" size="small" stripe>
        <el-table-column prop="title" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="severity" label="等级" width="100" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="message" label="最新说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="owner" label="负责人" width="120" />
        <el-table-column prop="lastSeenTime" label="最近发现" min-width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openHistory(row)">历史</el-button>
            <el-button link type="primary" @click="changeIssue(row, 'processing')">处理中</el-button>
            <el-button link type="success" @click="changeIssue(row, 'resolved')">解决</el-button>
            <el-button link type="info" @click="changeIssue(row, 'ignored')">忽略</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="ruleDialogVisible" :title="editingRuleId ? '编辑规则' : '新增规则'" width="640px">
      <el-form label-width="90px">
        <div class="form-row">
          <el-form-item label="编码">
            <el-input v-model="ruleForm.ruleCode" placeholder="snapshot_freshness_120m" />
          </el-form-item>
          <el-form-item label="名称">
            <el-input v-model="ruleForm.ruleName" placeholder="快照新鲜度检查" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="类型">
            <el-select v-model="ruleForm.checkType">
              <el-option label="快照缺失" value="snapshot_missing" />
              <el-option label="快照新鲜度" value="snapshot_freshness" />
              <el-option label="负值检查" value="negative_value" />
              <el-option label="日快照存在性" value="daily_snapshot_presence" />
            </el-select>
          </el-form-item>
          <el-form-item label="等级">
            <el-select v-model="ruleForm.severity">
              <el-option label="信息" value="info" />
              <el-option label="警告" value="warning" />
              <el-option label="错误" value="error" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="限定指标">
            <el-input v-model="ruleForm.metricCode" placeholder="留空表示全部指标" />
          </el-form-item>
          <el-form-item label="阈值">
            <el-input v-model="ruleForm.thresholdValue" placeholder="新鲜度分钟数，如 120" />
          </el-form-item>
        </div>
        <el-form-item label="说明">
          <el-input v-model="ruleForm.description" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="historyVisible" title="问题处理历史" size="520px">
      <el-table :data="issueHistory" size="small">
        <el-table-column prop="fromStatus" label="原状态" width="90" />
        <el-table-column prop="toStatus" label="新状态" width="90" />
        <el-table-column prop="operator" label="操作人" width="110" />
        <el-table-column prop="note" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" min-width="170" />
      </el-table>
    </el-drawer>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Pencil, Play, Plus } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const summary = reactive({ total: 0, passed: 0, failed: 0, warning: 0, latestCheckTime: '' })
const checks = ref([])
const rules = ref([])
const issues = ref([])
const issueTrend = ref([])
const issueStatus = ref('')
const issueHistory = ref([])
const historyVisible = ref(false)

const ruleDialogVisible = ref(false)
const editingRuleId = ref(null)
const ruleForm = reactive(emptyRuleForm())

onMounted(load)

function emptyRuleForm() {
  return {
    ruleCode: '',
    ruleName: '',
    checkType: 'snapshot_freshness',
    metricCode: '',
    thresholdValue: '',
    severity: 'warning',
    description: '',
    enabled: true
  }
}

async function load() {
  const [qualitySummary, qualityChecks, qualityRules, trend] = await Promise.all([
    api.qualitySummary(),
    api.qualityChecks(),
    api.qualityRules(),
    api.qualityIssueTrend()
  ])
  Object.assign(summary, qualitySummary)
  checks.value = qualityChecks
  rules.value = qualityRules
  issueTrend.value = trend
  await loadIssues()
}

async function loadIssues() {
  issues.value = await api.qualityIssues({ status: issueStatus.value })
}

async function runChecks() {
  await api.runQualityChecks()
  ElMessage.success('数据质量检查已完成')
  await load()
}

function openRuleCreate() {
  editingRuleId.value = null
  Object.assign(ruleForm, emptyRuleForm())
  ruleDialogVisible.value = true
}

function openRuleEdit(row) {
  editingRuleId.value = row.id
  Object.assign(ruleForm, row)
  ruleDialogVisible.value = true
}

async function saveRule() {
  if (editingRuleId.value) {
    await api.updateQualityRule(editingRuleId.value, ruleForm)
    ElMessage.success('质量规则已更新')
  } else {
    await api.createQualityRule(ruleForm)
    ElMessage.success('质量规则已新增')
  }
  ruleDialogVisible.value = false
  await load()
}

async function toggleRule(row) {
  await api.setQualityRuleEnabled(row.id, !row.enabled)
  ElMessage.success(row.enabled ? '规则已停用' : '规则已启用')
  await load()
}

async function changeIssue(row, status) {
  await api.updateQualityIssueStatus(row.id, {
    status,
    owner: row.owner || 'platform-ops',
    resolvedBy: 'platform-ops',
    resolutionNote: status === 'processing' ? '已进入处理' : '人工确认'
  })
  ElMessage.success('质量问题状态已更新')
  await load()
}

async function openHistory(row) {
  issueHistory.value = await api.qualityIssueHistory(row.id)
  historyVisible.value = true
}

function statusType(row) {
  if (row.status === 'failed') return row.severity === 'error' ? 'danger' : 'warning'
  return 'success'
}
</script>
