<template>
  <section class="page-grid">
    <div class="action-bar">
      <div><h2>AI 知识运营工作台</h2><p>问题池记录没答上的问题；在这里聚合缺口、核验来源、审核草稿并受控发布到 MF_EP。</p></div>
      <el-button type="primary" @click="aggregate">从问题池聚合</el-button>
    </div>
    <div class="panel">
      <h2 class="panel-title">待补全主题</h2>
      <el-table :data="gaps" size="small" empty-text="暂无待补主题，先从问题池聚合">
        <el-table-column prop="normalizedTopic" label="主题" min-width="220" />
        <el-table-column prop="occurrenceCount" label="相似问题" width="100" />
        <el-table-column prop="riskLevel" label="风险" width="100"><template #default="{ row }"><el-tag :type="riskType(row.riskLevel)">{{ row.riskLevel }}</el-tag></template></el-table-column>
        <el-table-column prop="status" label="状态" width="130" />
        <el-table-column prop="lastSeenAt" label="最近出现" width="180" />
        <el-table-column label="操作" width="250"><template #default="{ row }"><el-button link type="primary" @click="select(row)">查看依据</el-button><el-button link type="success" @click="research(row.id)">开始研究</el-button><el-button link type="info" @click="ignore(row.id)">忽略</el-button></template></el-table-column>
      </el-table>
    </div>
    <div v-if="selected" class="two-col">
      <div class="panel">
        <div class="panel-heading"><h2 class="panel-title">候选来源</h2><el-button size="small" @click="sourceDialog = true">录入来源</el-button></div>
        <el-table :data="sources" size="small" empty-text="请先核验并录入候选来源"><el-table-column prop="title" label="标题" min-width="180" /><el-table-column prop="publisher" label="发布方" width="120" /><el-table-column prop="authorityScore" label="可信度" width="90" /><el-table-column label="链接" width="80"><template #default="{ row }"><a :href="row.url" target="_blank">打开</a></template></el-table-column></el-table>
      </div>
      <div class="panel">
        <div class="panel-heading"><h2 class="panel-title">AI 草稿与预审</h2><el-button size="small" type="primary" @click="candidateDialog = true">录入草稿</el-button></div>
        <el-table :data="candidates" size="small" empty-text="暂无草稿"><el-table-column prop="contentType" label="类型" width="90" /><el-table-column prop="title" label="标题" min-width="160" /><el-table-column prop="riskLevel" label="风险" width="80" /><el-table-column prop="status" label="状态" width="120" /><el-table-column label="操作" width="340"><template #default="{ row }"><el-button v-if="!row.mfEpDraftId" link type="primary" @click="createDraft(row.id)">创建 MF_EP 草稿</el-button><el-button v-else-if="row.status === 'pending_publish'" link type="success" :disabled="isHighRisk(row.riskLevel)" @click="publish(row.id)">发布</el-button><el-button v-else-if="row.status === 'published'" link type="warning" @click="offline(row.id)">下线</el-button><el-button v-if="['published', 'offline'].includes(row.status)" link type="primary" @click="retrySync(row.id)">同步重试</el-button><el-button v-if="!['published', 'offline'].includes(row.status)" link type="danger" @click="reject(row.id)">退回</el-button><el-button link @click="showAudit(row.id)">审计</el-button></template></el-table-column></el-table>
        <p class="muted">高风险草稿不支持直接发布；发布和下线会真实调用 MF_EP，失败原因会保留在候选记录中。</p>
      </div>
    </div>
    <el-dialog v-model="sourceDialog" title="录入候选来源" width="520px"><el-form :model="sourceForm" label-width="76px"><el-form-item label="标题"><el-input v-model="sourceForm.title" /></el-form-item><el-form-item label="链接"><el-input v-model="sourceForm.url" /></el-form-item><el-form-item label="发布方"><el-input v-model="sourceForm.publisher" /></el-form-item><el-form-item label="可信度"><el-input-number v-model="sourceForm.authorityScore" :min="0" :max="100" /></el-form-item><el-form-item label="摘要"><el-input v-model="sourceForm.summary" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="sourceDialog = false">取消</el-button><el-button type="primary" @click="saveSource">保存</el-button></template></el-dialog>
    <el-dialog v-model="candidateDialog" title="录入 AI 草稿与预审" width="620px"><el-form :model="candidateForm" label-width="86px"><el-form-item label="类型"><el-select v-model="candidateForm.contentType"><el-option label="FAQ" value="faq" /><el-option label="百科" value="encyclopedia" /><el-option label="文章" value="article" /></el-select></el-form-item><el-form-item label="标题"><el-input v-model="candidateForm.title" /></el-form-item><el-form-item label="正文"><el-input v-model="candidateForm.content" type="textarea" :rows="6" /></el-form-item><el-form-item label="标签"><el-input v-model="candidateForm.tags" /></el-form-item><el-form-item label="预审结果"><el-input v-model="candidateForm.aiReviewJson" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="candidateDialog = false">取消</el-button><el-button type="primary" @click="saveCandidate">保存草稿</el-button></template></el-dialog>
    <el-dialog v-model="auditDialog" title="发布与同步审计" width="860px"><h3>发布动作</h3><el-table :data="auditLogs" size="small" empty-text="暂无发布记录"><el-table-column prop="action" label="动作" width="150" /><el-table-column prop="operator" label="操作人" width="140" /><el-table-column prop="mfEpContentId" label="MF_EP 内容" width="120" /><el-table-column prop="remark" label="说明" min-width="160" /><el-table-column prop="createTime" label="时间" width="180" /></el-table><h3>MF_EP 事件同步</h3><el-table :data="syncLogs" size="small" empty-text="暂无同步记录"><el-table-column prop="action" label="动作" width="90" /><el-table-column prop="deliveryStatus" label="事件状态" width="100"><template #default="{ row }"><el-tag :type="row.deliveryStatus === 'acknowledged' ? 'success' : row.deliveryStatus === 'failed' ? 'danger' : 'warning'">{{ row.deliveryStatus === 'acknowledged' ? '已确认' : row.deliveryStatus === 'failed' ? '失败' : '待确认' }}</el-tag></template></el-table-column><el-table-column prop="mfEpEventId" label="事件 ID" width="90" /><el-table-column prop="indexedDocuments" label="索引数" width="80" /><el-table-column prop="requestId" label="请求标识" min-width="160" /><el-table-column prop="error" label="失败原因" min-width="160" /><el-table-column prop="completedAt" label="完成时间" width="180" /></el-table></el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const gaps = ref([]); const selected = ref(null); const sources = ref([]); const candidates = ref([])
const sourceDialog = ref(false); const candidateDialog = ref(false)
const auditDialog = ref(false); const auditLogs = ref([]); const syncLogs = ref([])
const sourceForm = ref({ title: '', url: '', publisher: '', authorityScore: 80, summary: '' })
const candidateForm = ref({ contentType: 'faq', title: '', content: '', tags: '', aiReviewJson: '' })
const route = useRoute()
onMounted(async () => { await load(); const target = gaps.value.find(row => String(row.id) === String(route.query.gapId)); if (target) await select(target) })
async function load () { gaps.value = await api.knowledgeGaps() }
async function select (row) { selected.value = row; await loadDetail() }
async function loadDetail () { [sources.value, candidates.value] = await Promise.all([api.knowledgeSources(selected.value.id), api.knowledgeCandidates(selected.value.id)]) }
async function aggregate () { const count = await api.aggregateKnowledgeGaps(); ElMessage.success(`已处理 ${count} 条未解决问题`); await load() }
async function research (id) { await api.startGapResearch(id); ElMessage.success('已进入来源研究'); await load() }
async function ignore (id) { await api.ignoreKnowledgeGap(id); ElMessage.success('已忽略'); await load() }
async function saveSource () { await api.createKnowledgeSource(selected.value.id, sourceForm.value); sourceDialog.value = false; sourceForm.value = { title: '', url: '', publisher: '', authorityScore: 80, summary: '' }; await loadDetail() }
async function saveCandidate () { await api.createKnowledgeCandidate(selected.value.id, candidateForm.value); candidateDialog.value = false; candidateForm.value = { contentType: 'faq', title: '', content: '', tags: '', aiReviewJson: '' }; await loadDetail() }
async function createDraft (id) { const row = await api.createMfEpDraft(id); notifySync(row, 'MF_EP 草稿') ; await loadDetail() }
async function publish (id) { const row = await api.publishKnowledgeCandidate(id, { operator: 'datacenter-admin' }); notifySync(row, '发布') ; await loadDetail() }
async function offline (id) { const row = await api.offlineKnowledgeCandidate(id, { operator: 'datacenter-admin' }); notifySync(row, '下线') ; await loadDetail() }
async function reject (id) { await api.rejectKnowledgeCandidate(id); ElMessage.success('草稿已退回'); await loadDetail() }
async function showAudit (id) { [auditLogs.value, syncLogs.value] = await Promise.all([api.knowledgePublishLogs(id), api.knowledgeSyncLogs(id)]); auditDialog.value = true }
async function retrySync (id) { const result = await api.retryKnowledgeSync(id); result.success ? ElMessage.success('知识同步成功') : ElMessage.error(result.error || '知识同步失败'); await loadDetail() }
function notifySync (row, label) { row.status === 'sync_failed' ? ElMessage.error(row.lastError || `${label}失败`) : ElMessage.success(`${label}成功`) }
function isHighRisk (risk) { return ['high', 'urgent', '高', '高风险'].includes(String(risk).toLowerCase()) }
function riskType (risk) { return isHighRisk(risk) ? 'danger' : risk === 'medium' ? 'warning' : 'success' }
</script>
