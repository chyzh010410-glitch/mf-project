<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="q" inline>
        <el-form-item label="关键词"><el-input v-model="q.keyword" placeholder="用户名/姓名" clearable @keyup.enter="doSearch" style="width:180px" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="q.status" placeholder="全部" clearable style="width:100px">
          <el-option label="启用" :value="1" /><el-option label="禁用" :value="0" />
        </el-select></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="doSearch">搜索</el-button><el-button @click="doReset">重置</el-button></el-form-item>
      </el-form>
    </div>
    <div class="toolbar"><span class="toolbar-title">管理员管理</span><el-button type="primary" :icon="Plus" @click="handleAdd">新增管理员</el-button></div>
    <el-table v-loading="loading" :data="table" border stripe>
      <el-table-column prop="id" label="ID" width="65" align="center" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="role" label="角色" width="90" align="center">
        <template #default="{row}"><el-tag size="small" :type="row.role==='admin'?'danger':'info'">{{ row.role==='admin'?'管理员':'操作员' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{row}">
          <el-switch :model-value="row.status===1" inline-prompt active-text="启用" inactive-text="禁用" @change="(v)=>toggleStatus(row,v)" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" align="center"><template #default="{r}">{{ r?.createTime?.substring(0,16) || '-' }}</template></el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{row}">
          <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" background @current-change="fetch" @size-change="fetch" />
    <el-dialog v-model="vis" :title="edit?'编辑管理员':'新增管理员'" width="440px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="fr" :model="f" :rules="rls" label-width="80px">
        <el-form-item label="用户名" prop="username"><el-input v-model="f.username" placeholder="登录用户名" :disabled="edit" /></el-form-item>
        <el-form-item label="密码" :prop="edit?'':'password'">
          <el-input v-model="f.password" type="password" show-password :placeholder="edit?'留空则不改':'请输入密码'" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName"><el-input v-model="f.realName" placeholder="真实姓名" /></el-form-item>
        <el-form-item label="角色" prop="role"><el-select v-model="f.role" style="width:100%">
          <el-option label="管理员" value="admin" /><el-option label="操作员" value="operator" />
        </el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="vis=false">取消</el-button><el-button type="primary" :loading="saving" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'; import { ElMessage, ElMessageBox } from 'element-plus'; import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getAdminPage, getAdminDetail, saveAdmin, updateAdmin, toggleAdminStatus, deleteAdmin } from '@/api/admin'
const q=reactive({ keyword:'', status:null, page:1, size:10 }); const loading=ref(false); const table=ref([]); const total=ref(0)
const vis=ref(false); const edit=ref(false); const saving=ref(false); const fr=ref(null); const eid=ref(null)
const df=()=>({ username:'', password:'', realName:'', role:'operator' }); const f=reactive(df())
const rls={ username:[{required:true,message:'请输入用户名'}], password:[{required:true,message:'请输入密码',trigger:'blur'}], realName:[{required:true,message:'请输入姓名'}] }
const fetch=async()=>{ loading.value=true; try{ const p={...q}; if(p.status===null||p.status==='') delete p.status; if(!p.keyword) delete p.keyword
  const r=await getAdminPage(p); if(r.code===200&&r.data){ table.value=r.data.records||[]; total.value=r.data.total||0 } } catch{} finally{loading.value=false} }
const doSearch=()=>{ q.page=1; fetch() }; const doReset=()=>{ q.keyword=''; q.status=null; q.page=1; fetch() }
const handleAdd=()=>{ edit.value=false; eid.value=null; Object.assign(f,df()); rls.password[0].required=true; vis.value=true }
const handleEdit=async(r)=>{ edit.value=true; eid.value=r.id; rls.password[0].required=false
  try{ const x=await getAdminDetail(r.id); if(x.code===200&&x.data) Object.assign(f,{ username:x.data.username||'', password:'', realName:x.data.realName||'', role:x.data.role||'operator' }) } catch{}
  vis.value=true }
const handleSave=async()=>{ const v=await fr.value.validate().catch(()=>false); if(!v) return; saving.value=true
  try{ const d={realName:f.realName,role:f.role}; if(f.password) d.password=f.password
    edit.value?await updateAdmin(eid.value,d):await saveAdmin({username:f.username,password:f.password,realName:f.realName,role:f.role})
    ElMessage.success(edit.value?'更新成功':'新增成功'); vis.value=false; fetch() } catch{} finally{saving.value=false} }
const toggleStatus=async(r,v)=>{ try{ await toggleAdminStatus(r.id,v?1:0); r.status=v?1:0; ElMessage.success(v?'已启用':'已禁用') } catch{} }
const handleDelete=(r)=>{ ElMessageBox.confirm(`确认删除管理员「${r.username}」？`,'删除确认',{type:'warning'}).then(async()=>{ await deleteAdmin(r.id); ElMessage.success('已删除'); fetch() }).catch(()=>{}) }
onMounted(fetch)
</script>
