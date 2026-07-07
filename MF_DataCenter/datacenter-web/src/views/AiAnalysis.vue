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

const data = reactive({ conversationTotal: 0, uniqueUserTotal: 0, unresolvedTotal: 0, toolCallTotal: 0, sampleCandidateTotal: 0, frequentQuestions: [] })
const conversations = ref([])
const toolCalls = ref([])
const dialogVisible = ref(false)
const toolDialogVisible = ref(false)
const form = reactive({ source: 'MF_AgentService', question: '', answer: '', intent: 'fertilizer_advice', resolved: true, satisfaction: 5 })
const toolForm = reactive({ toolName: 'knowledge_search', requestSummary: '', responseSummary: '', success: true, errorMessage: '', durationMs: 120 })

onMounted(load)

async function load() {
  Object.assign(data, await api.aiAnalysis())
  conversations.value = await api.conversations()
  toolCalls.value = await api.toolCalls()
}

async function submit() {
  await api.createConversation(form)
  ElMessage.success('咨询日志已写入')
  dialogVisible.value = false
  form.question = ''
  form.answer = ''
  await load()
}

async function submitToolCall() {
  await api.createToolCall(toolForm)
  ElMessage.success('工具调用日志已写入')
  toolDialogVisible.value = false
  toolForm.requestSummary = ''
  toolForm.responseSummary = ''
  toolForm.errorMessage = ''
  await load()
}

const questionOption = computed(() => ({
  grid: { left: 92, right: 20, top: 18, bottom: 28 },
  tooltip: {},
  xAxis: { type: 'value' },
  yAxis: { type: 'category', data: data.frequentQuestions.map((item) => item.name) },
  series: [{ type: 'bar', data: data.frequentQuestions.map((item) => item.value), itemStyle: { color: '#2f7d4d', borderRadius: 4 } }]
}))
</script>
