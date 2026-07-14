import axios from 'axios'
import request from '@/utils/request'

const agent = axios.create({
  baseURL: '/agent-api',
  timeout: 60000
})

export function chatWithAgent(data) {
  return agent({
    url: '/api/agent/chat',
    method: 'post',
    data
  }).then((response) => response.data)
}

export function getAiConversations(params) {
  return request({
    url: '/client/ai/conversations',
    method: 'get',
    params,
    silentError: true
  })
}

export function deleteAiConversation(sessionId) {
  return request({
    url: `/client/ai/conversations/${encodeURIComponent(sessionId)}`,
    method: 'delete',
    silentError: true
  })
}

export async function streamChatWithAgent(data, handlers = {}) {
  const response = await fetch('/agent-api/api/agent/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(data)
  })
  if (!response.ok || !response.body) {
    throw new Error(`AI 服务请求失败（${response.status}）`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const dispatch = (block) => {
    let event = 'message'
    const dataLines = []
    block.split(/\r?\n/).forEach((line) => {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
    })
    const payload = dataLines.join('\n')
    if (!payload) return
    if (event === 'status') handlers.onStatus?.(payload)
    if (event === 'token') handlers.onToken?.(payload)
    if (event === 'result') {
      try {
        handlers.onResult?.(JSON.parse(payload))
      } catch {
        throw new Error('AI 服务返回的数据格式无效')
      }
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    let separator
    while ((separator = buffer.search(/\r?\n\r?\n/)) >= 0) {
      const block = buffer.slice(0, separator)
      buffer = buffer.slice(separator).replace(/^\r?\n\r?\n/, '')
      dispatch(block)
    }
    if (done) break
  }
}
