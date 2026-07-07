<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width:110px">
            <el-option label="待处理" value="pending" />
            <el-option label="已处理" value="handled" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryForm.type" placeholder="全部" clearable style="width:110px">
            <el-option label="建议" value="suggestion" />
            <el-option label="问题" value="problem" />
            <el-option label="投诉" value="complaint" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button><el-button @click="handleReset">重置</el-button></el-form-item>
      </el-form>
    </div>
    <div class="toolbar"><span class="toolbar-title">反馈处理</span></div>
    <el-table v-loading="loading" :data="tableData" border stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="userId" label="用户ID" width="80" align="center" />
      <el-table-column prop="type" label="类型" width="80" align="center">
        <template #default="{row}"><el-tag size="small">{{ row.type==='suggestion'?'建议':row.type==='problem'?'问题':'投诉' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="180" show-overflow-tooltip />
      <el-table-column prop="contact" label="联系方式" width="120" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{row}"><el-tag size="small" :type="row.status==='handled'?'success':'warning'">{{ row.status==='handled'?'已处理':'待处理' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="handlerReply" label="回复" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{row}">
          <el-button v-if="row.status!=='handled'" type="primary" link size="small" @click="openReply(row)">回复</el-button>
          <span v-else style="color:#bbb">已处理</span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" background
      @current-change="fetchData" @size-change="fetchData" />
    <el-dialog v-model="replyVisible" title="回复反馈" width="420px" :close-on-click-modal="false">
      <el-input v-model="replyText" type="textarea" :rows="4" placeholder="请输入回复内容" />
      <template #footer><el-button @click="replyVisible=false">取消</el-button><el-button type="primary" @click="doReply">发送回复</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'; import { ElMessage } from 'element-plus'; import { Search } from '@element-plus/icons-vue'
import { getFeedbackPage, replyFeedback } from '@/api/admin'
const queryForm=reactive({ status:'', type:'', page:1, size:10 })
const loading=ref(false); const tableData=ref([]); const total=ref(0)
const replyVisible=ref(false); const replyText=ref(''); const replyId=ref(null)
const fetchData=async()=>{ loading.value=true; try{ const p={...queryForm}; if(!p.status) delete p.status; if(!p.type) delete p.type
  const r=await getFeedbackPage(p); if(r.code===200&&r.data){ tableData.value=r.data.records||[]; total.value=r.data.total||0 } } catch{} finally{loading.value=false} }
const handleSearch=()=>{ queryForm.page=1; fetchData() }
const handleReset=()=>{ queryForm.status=''; queryForm.type=''; queryForm.page=1; fetchData() }
const openReply=(row)=>{ replyId.value=row.id; replyText.value=''; replyVisible.value=true }
const doReply=async()=>{ if(!replyText.value.trim()) return
  try{ await replyFeedback(replyId.value,replyText.value); ElMessage.success('已回复'); replyVisible.value=false; fetchData() } catch{} }
onMounted(fetchData)
</script>
