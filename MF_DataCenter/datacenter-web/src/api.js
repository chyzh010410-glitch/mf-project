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
  conversations: () => unwrap(client.get('/ai/conversations')),
  createConversation: (payload) => unwrap(client.post('/ai/conversations', payload)),
  toolCalls: () => unwrap(client.get('/ai/tool-calls')),
  createToolCall: (payload) => unwrap(client.post('/ai/tool-calls', payload)),
  unresolvedQuestions: (params) => unwrap(client.get('/ai/unresolved-questions', { params })),
  createUnresolvedQuestion: (payload) => unwrap(client.post('/ai/unresolved-questions', payload)),
  updateUnresolvedQuestion: (id, payload) => unwrap(client.patch(`/ai/unresolved-questions/${id}/status`, payload)),
  sampleCandidates: (params) => unwrap(client.get('/ai/sample-candidates', { params })),
  createSampleCandidate: (payload) => unwrap(client.post('/ai/sample-candidates', payload)),
  reviewSampleCandidate: (id, payload) => unwrap(client.patch(`/ai/sample-candidates/${id}/review`, payload)),
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
}
