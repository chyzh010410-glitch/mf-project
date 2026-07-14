import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  timeout: 10000
})

async function unwrap(promise) {
  const response = await promise
  return response.data.data
}

export const api = {
  systemStatus: () => unwrap(client.get('/system/status')),
  sourceContracts: () => unwrap(client.get('/source/contracts')),
  sourceCheck: () => unwrap(client.get('/source/check')),
  runSourceCheck: () => unwrap(client.post('/source/check')),
  dashboard: () => unwrap(client.get('/dashboard/overview')),
  products: () => unwrap(client.get('/analysis/products')),
  content: () => unwrap(client.get('/analysis/content')),
  merchants: () => unwrap(client.get('/analysis/merchants')),
  aiAnalysis: () => unwrap(client.get('/analysis/ai')),
  conversations: (params) => unwrap(client.get('/ai/conversations', { params })),
  conversationPage: (params) => unwrap(client.get('/ai/conversations/page', { params })),
  conversationTrace: (id) => unwrap(client.get(`/ai/conversations/${id}/trace`)),
  createConversation: (payload) => unwrap(client.post('/ai/conversations', payload)),
  updateConversationSatisfaction: (id, payload) => unwrap(client.patch(`/ai/conversations/${id}/satisfaction`, payload)),
  toolCalls: () => unwrap(client.get('/ai/tool-calls')),
  toolCallPage: (params) => unwrap(client.get('/ai/tool-calls/page', { params })),
  createToolCall: (payload) => unwrap(client.post('/ai/tool-calls', payload)),
  unresolvedQuestions: (params) => unwrap(client.get('/ai/unresolved-questions', { params })),
  unresolvedQuestionPage: (params) => unwrap(client.get('/ai/unresolved-questions/page', { params })),
  createUnresolvedQuestion: (payload) => unwrap(client.post('/ai/unresolved-questions', payload)),
  updateUnresolvedQuestion: (id, payload) => unwrap(client.patch(`/ai/unresolved-questions/${id}/status`, payload)),
  sampleCandidates: (params) => unwrap(client.get('/ai/sample-candidates', { params })),
  sampleCandidatePage: (params) => unwrap(client.get('/ai/sample-candidates/page', { params })),
  createSampleCandidate: (payload) => unwrap(client.post('/ai/sample-candidates', payload)),
  reviewSampleCandidate: (id, payload) => unwrap(client.patch(`/ai/sample-candidates/${id}/review`, payload)),
  evaluationSuites: () => unwrap(client.get('/ai/evaluations/suites')),
  evaluationCases: (params) => unwrap(client.get('/ai/evaluations/cases', { params })),
  createEvaluationSuite: (payload) => unwrap(client.post('/ai/evaluations/suites', payload)),
  createEvaluationCase: (payload) => unwrap(client.post('/ai/evaluations/cases', payload)),
  evaluationResults: () => unwrap(client.get('/ai/evaluations/results')),
  evaluationSummary: () => unwrap(client.get('/ai/evaluations/summary')),
  knowledgeGaps: () => unwrap(client.get('/ai/knowledge/gaps')),
  aggregateKnowledgeGaps: () => unwrap(client.post('/ai/knowledge/gaps/aggregate')),
  startGapResearch: (id) => unwrap(client.post(`/ai/knowledge/gaps/${id}/research`)),
  ignoreKnowledgeGap: (id) => unwrap(client.post(`/ai/knowledge/gaps/${id}/ignore`)),
  knowledgeSources: (id) => unwrap(client.get(`/ai/knowledge/gaps/${id}/sources`)),
  knowledgeCandidates: (gapId) => unwrap(client.get('/ai/knowledge/candidates', { params: { gapId } })),
  createKnowledgeSource: (id, payload) => unwrap(client.post(`/ai/knowledge/gaps/${id}/sources`, payload)),
  createKnowledgeCandidate: (id, payload) => unwrap(client.post(`/ai/knowledge/gaps/${id}/candidates`, payload)),
  createKnowledgeDraftFromSample: (id, payload) => unwrap(client.post(`/ai/knowledge/sample-candidates/${id}/draft`, payload)),
  createMfEpDraft: (id) => unwrap(client.post(`/ai/knowledge/candidates/${id}/mf-ep-draft`)),
  publishKnowledgeCandidate: (id, payload) => unwrap(client.post(`/ai/knowledge/candidates/${id}/publish`, payload)),
  offlineKnowledgeCandidate: (id, payload) => unwrap(client.post(`/ai/knowledge/candidates/${id}/offline`, payload)),
  knowledgePublishLogs: (id) => unwrap(client.get(`/ai/knowledge/candidates/${id}/publish-logs`)),
  knowledgeSyncLogs: (id) => unwrap(client.get(`/ai/knowledge/candidates/${id}/sync-logs`)),
  retryKnowledgeSync: (id) => unwrap(client.post(`/ai/knowledge/candidates/${id}/sync`)),
  rejectKnowledgeCandidate: (id) => unwrap(client.post(`/ai/knowledge/candidates/${id}/reject`)),
  metricDictionary: () => unwrap(client.get('/metrics/dictionary')),
  metricComputeRegistry: () => unwrap(client.get('/metrics/compute-registry')),
  setMetricComputeRegistryEnabled: (id, enabled) => unwrap(client.patch(`/metrics/compute-registry/${id}/enabled`, { enabled })),
  createMetricDefinition: (payload) => unwrap(client.post('/metrics/dictionary', payload)),
  updateMetricDefinition: (id, payload) => unwrap(client.put(`/metrics/dictionary/${id}`, payload)),
  setMetricDefinitionEnabled: (id, enabled) => unwrap(client.patch(`/metrics/dictionary/${id}/enabled`, { enabled })),
  metricLatest: () => unwrap(client.get('/metrics/latest')),
  metricQuery: (params) => unwrap(client.get('/metrics/query', { params })),
  refreshDailySnapshots: () => unwrap(client.post('/metrics/snapshots/daily/refresh')),
  refreshHourlySnapshots: () => unwrap(client.post('/metrics/snapshots/hourly/refresh')),
  qualitySummary: () => unwrap(client.get('/data-quality/summary')),
  qualityChecks: () => unwrap(client.get('/data-quality/checks')),
  qualityRules: () => unwrap(client.get('/data-quality/rules')),
  createQualityRule: (payload) => unwrap(client.post('/data-quality/rules', payload)),
  updateQualityRule: (id, payload) => unwrap(client.put(`/data-quality/rules/${id}`, payload)),
  setQualityRuleEnabled: (id, enabled) => unwrap(client.patch(`/data-quality/rules/${id}/enabled`, { enabled })),
  qualityIssues: (params) => unwrap(client.get('/data-quality/issues', { params })),
  qualityIssueTrend: () => unwrap(client.get('/data-quality/issues/trend')),
  qualityIssueHistory: (id) => unwrap(client.get(`/data-quality/issues/${id}/history`)),
  updateQualityIssueStatus: (id, payload) => unwrap(client.patch(`/data-quality/issues/${id}/status`, payload)),
  runQualityChecks: () => unwrap(client.post('/data-quality/run'))
  ,notifications: () => unwrap(client.get('/notifications'))
  ,notificationUnreadCount: () => unwrap(client.get('/notifications/unread-count'))
  ,markNotificationRead: (id) => unwrap(client.patch(`/notifications/${id}/read`))
}
