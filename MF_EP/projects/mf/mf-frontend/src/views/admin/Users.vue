<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="用户名/手机号/昵称" clearable @keyup.enter="handleSearch" style="width:220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width:110px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="toolbar"><span class="toolbar-title">用户列表</span></div>
    <el-table v-loading="loading" :data="tableData" border stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
      <el-table-column prop="nickname" label="昵称" width="100" />
      <el-table-column prop="phone" label="手机号" width="120" />
      <el-table-column prop="email" label="邮箱" width="140" show-overflow-tooltip />
      <el-table-column prop="points" label="积分" width="70" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{row}">
          <el-switch :model-value="row.status===1" inline-prompt active-text="正常" inactive-text="禁用"
            @change="(v)=>handleToggleStatus(row,v)" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="160" align="center">
        <template #default="{row}">{{ row.createTime?.substring(0,16) }}</template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" background
      @current-change="fetchData" @size-change="fetchData" />
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'; import { ElMessage } from 'element-plus'; import { Search } from '@element-plus/icons-vue'
import { getUserPage, toggleUserStatus } from '@/api/admin'
const queryForm = reactive({ keyword:'', status:null, page:1, size:10 })
const loading=ref(false); const tableData=ref([]); const total=ref(0)
const fetchData=async()=>{ loading.value=true; try{ const p={...queryForm}; if(p.status===null||p.status==='') delete p.status; if(!p.keyword) delete p.keyword
  const r=await getUserPage(p); if(r.code===200&&r.data){ tableData.value=r.data.records||[]; total.value=r.data.total||0 } } catch{} finally{loading.value=false} }
const handleSearch=()=>{ queryForm.page=1; fetchData() }
const handleReset=()=>{ queryForm.keyword=''; queryForm.status=null; queryForm.page=1; fetchData() }
const handleToggleStatus=async(row,v)=>{ try{ await toggleUserStatus(row.id,v?1:0); row.status=v?1:0; ElMessage.success(v?'已启用':'已禁用') } catch{} }
onMounted(fetchData)
</script>
