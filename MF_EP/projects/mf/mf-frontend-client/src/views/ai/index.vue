<template>
  <div class="page-container" style="max-width:700px">
    <h2 class="section-title">AI 智能客服</h2>
    <p style="color:#999;margin-bottom:20px">我有苗丰所有的植物百科和施肥知识，你可以问我任何养护问题。</p>

    <div class="chat-box" ref="chatBox">
      <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
        <div class="msg-bubble">{{ m.content }}</div>
        <div class="msg-sources" v-if="m.sources && m.sources.length">
          <span>参考：</span>
          <el-tag v-for="(s, si) in m.sources" :key="si" size="small" :type="s.type==='faq'?'warning':s.type==='article'?'info':'success'" style="margin-right:4px">
            {{ s.type==='faq' ? 'FAQ' : s.type==='article' ? '文章' : '百科' }} {{ s.name }}
          </el-tag>
        </div>
      </div>
      <div v-if="thinking" class="msg ai"><div class="msg-bubble">思考中...</div></div>
    </div>

    <div class="chat-input">
      <el-input v-model="input" placeholder="输入你的问题，如：苹果树春天怎么施肥？" @keyup.enter="send" size="large" />
      <el-button type="primary" size="large" :disabled="!input.trim() || thinking" @click="send" style="margin-left:8px">发送</el-button>
    </div>
  </div>
</template>
<script setup>
import { ref, nextTick } from 'vue'
import request from '@/utils/request'
const messages = ref([{ role: 'ai', content: '你好！我是苗丰智能客服，了解树木百科和施肥知识，有什么可以帮你的？' }])
const input = ref('')
const thinking = ref(false)
const chatBox = ref(null)

const send = async () => {
  if (!input.value.trim() || thinking.value) return
  const q = input.value.trim()
  input.value = ''
  messages.value.push({ role: 'user', content: q })
  thinking.value = true
  await nextTick(); scrollBottom()
  try {
    const res = await request({ url: '/client/ai/chat', method: 'post', data: { question: q } })
    if (res.code === 200 && res.data) {
      messages.value.push({ role: 'ai', content: res.data.answer, sources: res.data.sources })
    } else {
      messages.value.push({ role: 'ai', content: res.msg || '抱歉，暂时无法回答。' })
    }
  } catch {
    messages.value.push({ role: 'ai', content: 'AI 客服暂不可用，请稍后重试。' })
  } finally { thinking.value = false; await nextTick(); scrollBottom() }
}

const scrollBottom = () => {
  if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
}
</script>
<style scoped>
.chat-box { background: #f5f7fa; border-radius: 12px; padding: 20px; height: 420px; overflow-y: auto; margin-bottom: 16px; }
.msg { margin-bottom: 14px; display: flex; flex-direction: column; }
.msg.user { align-items: flex-end; }
.msg.ai { align-items: flex-start; }
.msg-bubble { max-width: 80%; padding: 10px 16px; border-radius: 12px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.msg.user .msg-bubble { background: #2d8c4a; color: #fff; }
.msg.ai .msg-bubble { background: #fff; border: 1px solid #e0e0e0; }
.msg-sources { margin-top: 6px; font-size: 12px; color: #999; display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.chat-input { display: flex; }
</style>
