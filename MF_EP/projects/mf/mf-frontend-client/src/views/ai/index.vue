<template>
  <div class="page-container ai-page">
    <div class="ai-header">
      <div>
        <h2 class="section-title">AI 智能客服</h2>
        <p class="ai-subtitle">我有苗丰植物百科和施肥知识，也可以协助查询商品、订单和商家入驻问题。</p>
      </div>
      <div class="ai-actions">
        <el-button @click="startNewSession">新建会话</el-button>
        <el-button type="danger" plain :disabled="messages.length <= 1" @click="clearSession">清空当前会话</el-button>
      </div>
    </div>

    <div class="chat-box" ref="chatBox">
      <div v-for="m in messages" :key="m.id" :class="['msg', m.role]">
        <div class="msg-bubble">{{ m.content }}</div>
        <div class="msg-sources" v-if="m.sources && m.sources.length">
          <span>参考：</span>
          <el-tag
            v-for="(s, si) in m.sources"
            :key="si"
            size="small"
            :type="s.type==='faq'?'warning':s.type==='article'?'info':'success'"
          >
            {{ s.title || s.name || s.sourceType || '来源' }}
          </el-tag>
        </div>
      </div>
      <div v-if="thinking" class="msg ai"><div class="msg-bubble">思考中...</div></div>
    </div>

    <div class="chat-input">
      <el-input v-model="input" placeholder="输入你的问题，如：苹果树春天怎么施肥？" @keyup.enter="send" size="large" />
      <el-button type="primary" size="large" :disabled="!input.trim() || thinking" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { chatWithAgent, deleteAiConversation, getAiConversations } from '@/api/agent'

const MAX_MESSAGES = 50
const authStore = useAuthStore()
const input = ref('')
const thinking = ref(false)
const chatBox = ref(null)
const sessionId = ref('')
const messages = ref([])

const userId = () => authStore.userInfo?.id || authStore.userInfo?.userId || 'guest'
const welcomeMessage = () => ({
  id: `welcome-${sessionId.value || 'default'}`,
  role: 'ai',
  content: '你好！我是苗丰智能客服，了解树木百科和施肥知识，有什么可以帮你的？',
  createdAt: new Date().toISOString(),
  sources: []
})
const activeSessionKey = () => `mf-ai-active-session:${userId()}`
const cacheKey = () => `mf-ai-chat:${userId()}:${sessionId.value}`

const initSession = () => {
  const stored = localStorage.getItem(activeSessionKey())
  sessionId.value = stored || `mf-ep-client-${userId()}-default`
  localStorage.setItem(activeSessionKey(), sessionId.value)
}

const loadLocalMessages = () => {
  try {
    const cached = JSON.parse(localStorage.getItem(cacheKey()) || '[]')
    messages.value = Array.isArray(cached) && cached.length ? sanitizeMessages(cached) : [welcomeMessage()]
  } catch {
    messages.value = [welcomeMessage()]
  }
}

const saveLocalMessages = () => {
  const safe = sanitizeMessages(messages.value).slice(-MAX_MESSAGES)
  messages.value = safe
  localStorage.setItem(cacheKey(), JSON.stringify(safe))
}

const sanitizeMessages = (items) => items
  .filter(item => item && ['user', 'ai'].includes(item.role) && item.content)
  .map(item => ({
    id: String(item.id || `${item.role}-${Date.now()}-${Math.random()}`),
    role: item.role,
    content: String(item.content),
    createdAt: item.createdAt || new Date().toISOString(),
    sources: Array.isArray(item.sources) ? item.sources : []
  }))

const loadServerMessages = async () => {
  try {
    const res = await getAiConversations({ sessionId: sessionId.value, page: 1, pageSize: MAX_MESSAGES })
    const serverMessages = toMessages(res.data?.items || [])
    if (serverMessages.length) {
      messages.value = mergeMessages(messages.value, serverMessages)
      saveLocalMessages()
      await nextTick()
      scrollBottom()
    }
  } catch (error) {
    console.debug('AI history sync skipped; local conversation is still available.', error)
  }
}

const toMessages = (items) => items.flatMap(item => {
  const createdAt = item.createdAt || item.createTime || new Date().toISOString()
  return [
    { id: `server-${item.id}-user`, role: 'user', content: item.question, createdAt, sources: [] },
    { id: `server-${item.id}-ai`, role: 'ai', content: item.answer || '已记录该问题。', createdAt, sources: [] }
  ].filter(message => message.content)
})

const mergeMessages = (localMessages, serverMessages) => {
  const map = new Map()
  for (const message of [...serverMessages, ...localMessages]) {
    const key = `${message.role}:${message.id}:${message.content}`
    map.set(key, message)
  }
  return Array.from(map.values())
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    .slice(-MAX_MESSAGES)
}

const send = async () => {
  if (!input.value.trim() || thinking.value) return
  const q = input.value.trim()
  input.value = ''
  messages.value.push({ id: `local-user-${Date.now()}`, role: 'user', content: q, createdAt: new Date().toISOString(), sources: [] })
  saveLocalMessages()
  thinking.value = true
  await nextTick()
  scrollBottom()
  try {
    const res = await chatWithAgent({
      sessionId: sessionId.value,
      message: q,
      userId: userId() === 'guest' ? undefined : String(userId()),
      userType: 'client',
      authToken: authStore.token ? `Bearer ${authStore.token}` : undefined
    })
    messages.value.push({
      id: res?.conversationId ? `server-${res.conversationId}-ai` : `local-ai-${Date.now()}`,
      role: 'ai',
      content: res?.answer || '抱歉，暂时无法回答。',
      createdAt: new Date().toISOString(),
      sources: res?.sources || []
    })
  } catch {
    messages.value.push({ id: `local-ai-${Date.now()}`, role: 'ai', content: 'AI 客服暂不可用，请稍后重试。', createdAt: new Date().toISOString(), sources: [] })
  } finally {
    thinking.value = false
    saveLocalMessages()
    await nextTick()
    scrollBottom()
  }
}

const startNewSession = () => {
  sessionId.value = `mf-ep-client-${userId()}-${crypto.randomUUID()}`
  localStorage.setItem(activeSessionKey(), sessionId.value)
  messages.value = [welcomeMessage()]
  saveLocalMessages()
}

const clearSession = async () => {
  await ElMessageBox.confirm('确认清空当前 AI 客服会话？', '清空会话', { type: 'warning' })
  try {
    await deleteAiConversation(sessionId.value)
  } catch (error) {
    console.debug('AI server history clear skipped; local conversation will be cleared.', error)
  }
  localStorage.removeItem(cacheKey())
  messages.value = [welcomeMessage()]
  saveLocalMessages()
  ElMessage.success('当前会话已清空')
}

const scrollBottom = () => {
  if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
}

onMounted(async () => {
  initSession()
  loadLocalMessages()
  await nextTick()
  scrollBottom()
  await loadServerMessages()
})
</script>

<style scoped>
.ai-page { max-width: 760px; }
.ai-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 14px; }
.ai-subtitle { color: var(--color-text-secondary); margin-bottom: 0; line-height: 1.7; }
.ai-actions { display: flex; gap: 8px; flex-shrink: 0; }
.chat-box { background: var(--color-surface-soft); border: 1px solid var(--color-border); border-radius: var(--radius-panel); padding: 20px; height: 420px; overflow-y: auto; margin-bottom: 16px; }
.msg { margin-bottom: 14px; display: flex; flex-direction: column; }
.msg.user { align-items: flex-end; }
.msg.ai { align-items: flex-start; }
.msg-bubble { max-width: 80%; padding: 10px 16px; border-radius: 12px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.msg.user .msg-bubble { background: var(--color-primary); color: #fff; }
.msg.ai .msg-bubble { background: #fff; border: 1px solid var(--color-border); }
.msg-sources { margin-top: 6px; font-size: 12px; color: var(--color-text-muted); display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.chat-input { display: flex; gap: 8px; }
@media (max-width: 720px) {
  .ai-header { flex-direction: column; }
  .ai-actions { width: 100%; }
  .ai-actions .el-button { flex: 1; }
}
</style>
