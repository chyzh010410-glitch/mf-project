<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="q" inline>
        <el-form-item label="关键词"><el-input v-model="q.keyword" placeholder="搜索标题" clearable @keyup.enter="doSearch" style="width:180px" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="q.status" placeholder="全部" clearable style="width:110px">
          <el-option label="进行中" value="active" /><el-option label="已结束" value="ended" />
        </el-select></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="doSearch">搜索</el-button><el-button @click="doReset">重置</el-button></el-form-item>
      </el-form>
    </div>
    <div class="toolbar"><span class="toolbar-title">活动管理</span><el-button type="primary" :icon="Plus" @click="handleAdd">新增活动</el-button></div>
    <el-table v-loading="loading" :data="table" border stripe>
      <el-table-column prop="id" label="ID" width="65" align="center" />
      <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="90" align="center">
        <template #default="{row}"><el-tag size="small">{{ row.type==='discount'?'折扣':row.type==='seckill'?'秒杀':row.type==='new'?'新品':'活动' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{row}"><el-tag size="small" :type="row.status==='active'?'success':'info'">{{ row.status==='active'?'进行中':'已结束' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="isBanner" label="Banner" width="70" align="center">
        <template #default="{row}"><el-tag size="small" :type="row.isBanner===1?'danger':'info'">{{ row.isBanner===1?'是':'否' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始" width="140" align="center"><template #default="{row}">{{ row.startTime?.substring(0,16) }}</template></el-table-column>
      <el-table-column prop="endTime" label="结束" width="140" align="center"><template #default="{row}">{{ row.endTime?.substring(0,16) }}</template></el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{row}">
          <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.status==='active'" type="warning" link size="small" @click="endActivity(row)">结束</el-button>
          <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" background @current-change="fetch" @size-change="fetch" />
    <el-dialog v-model="vis" :title="edit?'编辑活动':'新增活动'" width="580px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="fr" :model="f" :rules="rls" label-width="80px">
        <el-form-item label="标题" prop="title"><el-input v-model="f.title" /></el-form-item>
        <el-form-item label="类型" prop="type"><el-select v-model="f.type" style="width:100%">
          <el-option label="限时折扣" value="discount" /><el-option label="秒杀" value="seckill" /><el-option label="新品预售" value="new" />
        </el-select></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="f.startTime" type="datetime" placeholder="选择" style="width:100%" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="f.endTime" type="datetime" placeholder="选择" style="width:100%" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="封面图"><el-input v-model="f.coverImage" placeholder="图片URL" /></el-form-item>
        <el-form-item label="活动规则"><el-input v-model="f.ruleJson" type="textarea" :rows="2" placeholder='如 {"discountRate":0.8}' /></el-form-item>
        <el-form-item label="描述"><el-input v-model="f.description" type="textarea" :rows="2" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="Banner"><el-switch v-model="f.isBanner" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="f.sortOrder" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="vis=false">取消</el-button><el-button type="primary" :loading="saving" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'; import { ElMessage, ElMessageBox } from 'element-plus'; import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getActivityPage, getActivityDetail, saveActivity, updateActivity, deleteActivity, toggleActivityStatus } from '@/api/admin'
const q=reactive({ keyword:'', status:'', type:'', page:1, size:10 }); const loading=ref(false); const table=ref([]); const total=ref(0)
const vis=ref(false); const edit=ref(false); const saving=ref(false); const fr=ref(null); const eid=ref(null)
const df=()=>({ title:'', type:'', coverImage:'', description:'', ruleJson:'', startTime:'', endTime:'', isBanner:0, sortOrder:0 })
const f=reactive(df()); const rls={ title:[{required:true,message:'请输入标题'}], type:[{required:true,message:'请选择类型'}], startTime:[{required:true,message:'请选择开始时间'}], endTime:[{required:true,message:'请选择结束时间'}] }
const fetch=async()=>{ loading.value=true; try{ const p={...q}; if(!p.status) delete p.status; if(!p.keyword) delete p.keyword; if(!p.type) delete p.type
  const r=await getActivityPage(p); if(r.code===200&&r.data){ table.value=r.data.records||[]; total.value=r.data.total||0 } } catch{} finally{loading.value=false} }
const doSearch=()=>{ q.page=1; fetch() }
const doReset=()=>{ q.keyword=''; q.status=''; q.type=''; q.page=1; fetch() }
const handleAdd=()=>{ edit.value=false; eid.value=null; Object.assign(f,df()); vis.value=true }
const handleEdit=async(r)=>{ edit.value=true; eid.value=r.id
  try{ const x=await getActivityDetail(r.id); if(x.code===200&&x.data) Object.assign(f,{ title:x.data.title||'', type:x.data.type||'', coverImage:x.data.coverImage||'', description:x.data.description||'', ruleJson:x.data.ruleJson||'', startTime:x.data.startTime||'', endTime:x.data.endTime||'', isBanner:x.data.isBanner??0, sortOrder:x.data.sortOrder??0 }) } catch{}
  vis.value=true }
const handleSave=async()=>{ const v=await fr.value.validate().catch(()=>false); if(!v) return; saving.value=true
  try{ edit.value?await updateActivity(eid.value,f):await saveActivity(f); ElMessage.success(edit.value?'更新成功':'新增成功'); vis.value=false; fetch() } catch{} finally{saving.value=false} }
const handleDelete=(r)=>{ ElMessageBox.confirm(`确认删除「${r.title}」？`,'删除确认',{type:'warning'}).then(async()=>{ await deleteActivity(r.id); ElMessage.success('已删除'); fetch() }).catch(()=>{}) }
const endActivity=async(r)=>{ try{ await toggleActivityStatus(r.id,'ended'); r.status='ended'; ElMessage.success('已结束') } catch{} }
onMounted(fetch)
</script>
