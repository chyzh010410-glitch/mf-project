<template>
  <div class="page-container">
    <h2 class="section-title">消息中心</h2>
    <div v-if="messages.length===0&&!loading" style="padding:80px;text-align:center;color:#999">暂无消息</div>
    <div v-for="m in messages" :key="m.id" class="msg-item" :class="{unread:m.isRead===0}" @click="readMsg(m)">
      <div class="msg-header">
        <el-tag size="small" :type="m.type==='system'?'':m.type==='order'?'warning':m.type==='activity'?'danger':'info'">
          {{ m.type==='system'?'系统通知':m.type==='order'?'订单消息':m.type==='activity'?'活动推送':'互动消息' }}
        </el-tag>
        <span class="msg-dot" v-if="m.isRead===0"></span>
        <span class="msg-time">{{ m.createTime?.substring(0,16) }}</span>
      </div>
      <h4 class="msg-title">{{ m.title }}</h4>
      <p class="msg-content">{{ m.content }}</p>
    </div>
    <el-pagination v-if="total>size" v-model:current-page="page" :page-size="size" :total="total"
      :page-sizes="[10,20]" background layout="prev,pager,next" style="justify-content:center;margin-top:24px"
      @current-change="fetchData" @size-change="fetchData" />
  </div>
</template>
<script setup>
import { ref, onMounted, inject } from 'vue'
import { getMessages, markRead } from '@/api/message'
const messages=ref([]); const total=ref(0); const page=ref(1); const size=ref(10); const loading=ref(false)
const fetchUnreadCount=inject('fetchUnreadCount',()=>{})

const fetchData=async()=>{ loading.value=true
  try{ const r=await getMessages({page:page.value,size:size.value}); if(r.code===200&&r.data){ messages.value=r.data.records||[]; total.value=r.data.total||0 } } catch{} finally{loading.value=false} }

const readMsg=async(m)=>{ if(m.isRead===0){ try{ await markRead(m.id); m.isRead=1; fetchUnreadCount() } catch{} } }

onMounted(fetchData)
</script>
<style scoped>
.msg-item{ background:#fff; border-radius:8px; padding:16px 20px; margin-bottom:12px; border:1px solid #ebeef5; cursor:pointer; transition:background .2s }
.msg-item:hover{ background:#fafafa }
.msg-item.unread{ border-left:3px solid #2d8c4a; background:#f0f9f4 }
.msg-header{ display:flex; align-items:center; gap:8px; margin-bottom:8px }
.msg-dot{ width:8px; height:8px; border-radius:50%; background:#e74c3c }
.msg-time{ font-size:12px; color:#bbb; margin-left:auto }
.msg-title{ font-size:15px; color:#333; margin:0 0 6px }
.msg-content{ font-size:13px; color:#666; margin:0; line-height:1.6 }
</style>
