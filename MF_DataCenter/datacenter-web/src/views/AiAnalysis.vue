<template>
  <section class="page-grid">
    <div class="metric-grid">
      <div class="metric-card">
        <span>咨询次数</span>
        <strong>{{ data.conversationTotal }}</strong>
        <p>客服 Agent 写入</p>
      </div>
      <div class="metric-card">
        <span>独立咨询用户</span>
        <strong>{{ data.uniqueUserTotal }}</strong>
        <p>按 userId 去重</p>
      </div>
      <div class="metric-card">
        <span>未解决问题</span>
        <strong>{{ data.unresolvedTotal }}</strong>
        <p>进入问题池</p>
      </div>
      <div class="metric-card">
        <span>工具调用次数</span>
        <strong>{{ data.toolCallTotal }}</strong>
        <p>Agent 工具链路</p>
      </div>
      <div class="metric-card">
        <span>样本候选</span>
        <strong>{{ data.sampleCandidateTotal }}</strong>
        <p>待人工审核</p>
      </div>
      <div class="metric-card"><span>工具成功率</span><strong>{{ data.toolSuccessRate }}%</strong><p>真实工具调用结果</p></div>
      <div class="metric-card"><span>未解决率</span><strong>{{ data.unresolvedRate }}%</strong><p>需要知识补充</p></div>
      <div class="metric-card"><span>样本通过率</span><strong>{{ data.sampleApprovalRate }}%</strong><p>审核通过的样本</p></div>
      <div class="metric-card"><span>评测通过率</span><strong>{{ evaluation.passRate }}%</strong><p>{{ evaluation.passed }}/{{ evaluation.total }} 条评测结果</p></div>
    </div>
    <div class="two-col">
      <div class="panel">
        <h2 class="panel-title">高频问题</h2>
        <ChartBox :option="questionOption" />
      </div>
      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">最新咨询日志</h2>
          <el-button type="success" :icon="Plus" @click="dialogVisible = true">写入</el-button>
        </div>
        <el-table :data="conversations" size="small">
          <el-table-column prop="question" label="问题" min-width="180" />
          <el-table-column prop="intent" label="意图" width="140" />
          <el-table-column prop="resolved" label="解决" width="80">
            <template #default="{ row }">
              <el-tag :type="row.resolved ? 'success' : 'warning'" effect="plain">{{ row.resolved ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <TablePager v-bind="conversationPage" @change="changeConversationPage" />
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h2 class="panel-title">最新工具调用</h2>
        <el-button :icon="Wrench" @click="toolDialogVisible = true">写入工具日志</el-button>
      </div>
      <el-table :data="toolCalls" size="small">
        <el-table-column prop="toolName" label="工具" width="190" />
        <el-table-column prop="requestSummary" label="请求摘要" min-width="180" show-overflow-tooltip />
        <el-table-column prop="responseSummary" label="响应摘要" min-width="180" show-overflow-tooltip />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="success" label="成功" width="80">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" effect="plain">{{ row.success ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <TablePager v-bind="toolPage" @change="changeToolPage" />
    </div>

    <div class="panel">
      <div class="panel-header"><h2 class="panel-title">咨询趋势与会话追溯</h2><div class="toolbar compact-toolbar"><el-select v-model="filters.source" clearable placeholder="来源" @change="applyFilters"><el-option label="AgentService" value="MF_AgentService" /></el-select><el-input v-model="filters.intent" clearable placeholder="意图" @keyup.enter="applyFilters" /></div></div>
      <ChartBox :option="trendOption" />
      <el-table :data="conversations" size="small" @row-click="openTrace">
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="150" />
        <el-table-column prop="intent" label="意图" width="140" />
        <el-table-column prop="createTime" label="写入时间" width="180" />
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="写入 AI 咨询日志" width="620px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="来源"><el-input v-model="form.source" /></el-form-item>
        <el-form-item label="问题"><el-input v-model="form.question" type="textarea" /></el-form-item>
        <el-form-item label="回答"><el-input v-model="form.answer" type="textarea" /></el-form-item>
        <div class="form-row">
          <el-form-item label="意图"><el-input v-model="form.intent" /></el-form-item>
          <el-form-item label="满意度"><el-input-number v-model="form.satisfaction" :min="1" :max="5" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="success" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="traceVisible" title="会话追溯" size="620px">
      <template v-if="trace"><h3>{{ trace.conversation.question }}</h3><p>{{ trace.conversation.answer || '暂无回答内容' }}</p><h4>工具调用</h4><el-table :data="trace.toolCalls" size="small"><el-table-column prop="toolName" label="工具" /><el-table-column prop="success" label="结果"><template #default="{ row }"><el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag></template></el-table-column></el-table><h4>问题池</h4><el-table :data="trace.unresolvedQuestions" size="small"><el-table-column prop="status" label="状态" /><el-table-column prop="reason" label="原因" /></el-table><h4>样本池</h4><el-table :data="trace.sampleCandidates" size="small"><el-table-column prop="reviewStatus" label="审核" /><el-table-column prop="recommendedForKnowledge" label="推荐入库"><template #default="{ row }">{{ row.recommendedForKnowledge ? '是' : '否' }}</template></el-table-column></el-table></template>
    </el-drawer>

    <el-dialog v-model="toolDialogVisible" title="写入工具调用日志" width="640px">
      <el-form :model="toolForm" label-width="104px">
        <el-form-item label="工具名称"><el-input v-model="toolForm.toolName" /></el-form-item>
        <el-form-item label="请求摘要"><el-input v-model="toolForm.requestSummary" type="textarea" /></el-form-item>
        <el-form-item label="响应摘要"><el-input v-model="toolForm.responseSummary" type="textarea" /></el-form-item>
        <div class="form-row">
          <el-form-item label="调用成功"><el-switch v-model="toolForm.success" /></el-form-item>
          <el-form-item label="耗时"><el-input-number v-model="toolForm.durationMs" :min="0" /></el-form-item>
        </div>
        <el-form-item label="错误信息"><el-input v-model="toolForm.errorMessage" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="toolDialogVisible = false">取消</el-button>
        <el-button type="success" @click="submitToolCall">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Wrench } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import ChartBox from '../components/ChartBox.vue'
import TablePager from '../components/TablePager.vue'

const data = reactive({ conversationTotal: 0, uniqueUserTotal: 0, unresolvedTotal: 0, toolCallTotal: 0, sampleCandidateTotal: 0, frequentQuestions: [], conversationTrend: [], toolSuccessRate: 0, unresolvedRate: 0, sampleApprovalRate: 0 })
const evaluation = reactive({ total: 0, passed: 0, passRate: 0 })
const conversations = ref([])
const toolCalls = ref([])
const conversationPage = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const toolPage = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const filters = reactive({ source: '', intent: '' })
const traceVisible = ref(false)
const trace = ref(null)
const dialogVisible = ref(false)
const toolDialogVisible = ref(false)
const form = reactive({ source: 'MF_AgentService', question: '', answer: '', intent: 'fertilizer_advice', resolved: true, satisfaction: 5 })
const toolForm = reactive({ toolName: 'knowledge_search', requestSummary: '', responseSummary: '', success: true, errorMessage: '', durationMs: 120 })

onMounted(loadAll)

async function loadSummary() {
  const [analysis, evaluationSummary] = await Promise.all([api.aiAnalysis(), api.evaluationSummary()])
  Object.assign(data, analysis)
  Object.assign(evaluation, evaluationSummary)
}

async function loadConversations() {
  const conversationResult = await api.conversationPage({ ...filters, pageNo: conversationPage.pageNo, pageSize: conversationPage.pageSize })
  conversations.value = conversationResult.records
  Object.assign(conversationPage, conversationResult)
}

async function loadToolCalls() {
  const toolResult = await api.toolCallPage({ pageNo: toolPage.pageNo, pageSize: toolPage.pageSize })
  toolCalls.value = toolResult.records
  Object.assign(toolPage, toolResult)
}

async function loadAll() { await Promise.all([loadSummary(), loadConversations(), loadToolCalls()]) }
async function changeConversationPage (page) { Object.assign(conversationPage, page); await loadConversations() }
async function changeToolPage (page) { Object.assign(toolPage, page); await loadToolCalls() }
async function applyFilters() { conversationPage.pageNo = 1; await loadConversations() }

async function openTrace(row) { trace.value = await api.conversationTrace(row.id); traceVisible.value = true }

async function submit() {
  await api.createConversation(form)
  ElMessage.success('咨询日志已写入')
  dialogVisible.value = false
  form.question = ''
  form.answer = ''
  await loadAll()
}

async function submitToolCall() {
  await api.createToolCall(toolForm)
  ElMessage.success('工具调用日志已写入')
  toolDialogVisible.value = false
  toolForm.requestSummary = ''
  toolForm.responseSummary = ''
  toolForm.errorMessage = ''
  await loadAll()
}

const questionOption = computed(() => ({
  grid: { left: 92, right: 20, top: 18, bottom: 28 },
  tooltip: {},
  xAxis: { type: 'value' },
  yAxis: { type: 'category', data: (data.frequentQuestions || []).map((item) => item.name) },
  series: [{ type: 'bar', data: (data.frequentQuestions || []).map((item) => item.value), itemStyle: { color: '#2f7d4d', borderRadius: 4 } }]
}))

const trendOption = computed(() => ({ grid: { left: 42, right: 18, top: 18, bottom: 30 }, tooltip: { trigger: 'axis' }, xAxis: { type: 'category', data: (data.conversationTrend || []).map(item => item.date) }, yAxis: { type: 'value' }, series: [{ type: 'line', smooth: true, data: (data.conversationTrend || []).map(item => item.value), itemStyle: { color: '#2f7d4d' }, areaStyle: { opacity: .12 } }] }))
</script>
