<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="q" inline>
        <el-form-item label="模块"><el-select v-model="q.module" placeholder="全部模块" clearable style="width:130px">
          <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
        </el-select></el-form-item>
        <el-form-item label="关键词"><el-input v-model="q.keyword" placeholder="操作人/操作/目标" clearable @keyup.enter="doSearch" style="width:200px" /></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="doSearch">搜索</el-button><el-button @click="doReset">重置</el-button></el-form-item>
      </el-form>
    </div>
    <div class="toolbar"><span class="toolbar-title">系统日志</span></div>
    <el-table v-loading="loading" :data="table" border stripe size="small">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="module" label="模块" width="100" align="center" />
      <el-table-column prop="action" label="操作" width="80" align="center" />
      <el-table-column prop="operatorName" label="操作人" width="100" align="center" />
      <el-table-column prop="target" label="目标" width="120" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="130" align="center" />
      <el-table-column prop="result" label="结果" width="70" align="center">
        <template #default="{row}"><el-tag size="small" :type="row.result==='success'?'success':'danger'">{{ row.result==='success'?'成功':'失败' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="costTime" label="耗时" width="70" align="center"><template #default="{r}">{{ r?.costTime != null ? r.costTime + 'ms' : '-' }}</template></el-table-column>
      <el-table-column prop="createTime" label="时间" width="160" align="center"><template #default="{r}">{{ r?.createTime?.substring(0,19) || '-' }}</template></el-table-column>
    
    </el-table>
    <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" background @current-change="fetch" @size-change="fetch" />
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'; import { Search } from '@element-plus/icons-vue'
import { getLogPage } from '@/api/admin'
const q=reactive({ module:'', keyword:'', page:1, size:10 }); const loading=ref(false); const table=ref([]); const total=ref(0)
const modules=['商品管理','分类管理','百科管理','文章管理','评论管理','上传审核','用户管理','反馈处理','FAQ管理','消息推送','活动管理','平台配置','管理员管理']
const fetch=async()=>{ loading.value=true; try{ const p={...q}; if(!p.module) delete p.module; if(!p.keyword) delete p.keyword
  const r=await getLogPage(p); if(r.code===200&&r.data){ table.value=r.data.records||[]; total.value=r.data.total||0 } } catch{} finally{loading.value=false} }
const doSearch=()=>{ q.page=1; fetch() }; const doReset=()=>{ q.module=''; q.keyword=''; q.page=1; fetch() }
onMounted(fetch)
</script>
