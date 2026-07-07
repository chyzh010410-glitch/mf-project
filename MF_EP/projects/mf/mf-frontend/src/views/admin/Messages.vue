<template>
  <div class="page-container">
    <div class="toolbar"><span class="toolbar-title">消息推送</span><el-button type="primary" :icon="Plus" @click="openSend">发送消息</el-button></div>
    <el-table v-loading="loading" :data="tableData" border stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="userId" label="用户ID" width="80" align="center" />
      <el-table-column prop="title" label="标题" width="140" show-overflow-tooltip />
      <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="100" align="center">
        <template #default="{row}">
          <el-tag size="small">{{ row.type==='system'?'系统':row.type==='order'?'订单':row.type==='activity'?'活动':'互动' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isRead" label="已读" width="70" align="center">
        <template #default="{row}">{{ row.isRead===1?'是':'否' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="发送时间" width="160" align="center">
        <template #default="{row}">{{ row.createTime?.substring(0,16) }}</template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" background
      @current-change="fetchData" @size-change="fetchData" />
    <el-dialog v-model="sendVisible" title="发送消息" width="480px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="用户ID" prop="userIds">
          <el-input v-model="userIdsText" placeholder="多个用逗号分隔，如 1,2,3" />
        </el-form-item>
        <el-form-item label="标题" prop="title"><el-input v-model="formData.title" placeholder="消息标题" /></el-form-item>
        <el-form-item label="内容" prop="content"><el-input v-model="formData.content" type="textarea" :rows="4" placeholder="消息内容" /></el-form-item>
        <el-form-item label="类型" prop="type"><el-select v-model="formData.type" placeholder="请选择" style="width:100%">
          <el-option label="系统通知" value="system" /><el-option label="订单消息" value="order" /><el-option label="活动推送" value="activity" /><el-option label="互动消息" value="interaction" />
        </el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="sendVisible=false">取消</el-button><el-button type="primary" :loading="sending" @click="doSend">发送</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'; import { ElMessage } from 'element-plus'; import { Plus } from '@element-plus/icons-vue'
import { getMessagePage, sendMessage } from '@/api/admin'
const queryForm=reactive({ page:1, size:10 })
const loading=ref(false); const tableData=ref([]); const total=ref(0)
const sendVisible=ref(false); const sending=ref(false); const formRef=ref(null); const userIdsText=ref('')
const formData=reactive({ title:'', content:'', type:'' })
const rules={ title:[{required:true,message:'请输入标题'}], content:[{required:true,message:'请输入内容'}], type:[{required:true,message:'请选择类型'}] }
const fetchData=async()=>{ loading.value=true; try{ const r=await getMessagePage(queryForm); if(r.code===200&&r.data){ tableData.value=r.data.records||[]; total.value=r.data.total||0 } } catch{} finally{loading.value=false} }
const openSend=()=>{ userIdsText.value=''; Object.assign(formData,{title:'',content:'',type:''}); sendVisible.value=true }
const doSend=async()=>{ const v=await formRef.value.validate().catch(()=>false); if(!v) return
  if(!userIdsText.value.trim()) return ElMessage.warning('请输入用户ID')
  const ids=userIdsText.value.split(',').map(s=>Number(s.trim())).filter(Boolean)
  if(!ids.length) return ElMessage.warning('请输入有效的用户ID')
  sending.value=true; try{ await sendMessage({...formData,userIds:ids}); ElMessage.success('发送成功'); sendVisible.value=false; fetchData() } catch{} finally{sending.value=false} }
onMounted(fetchData)
</script>
