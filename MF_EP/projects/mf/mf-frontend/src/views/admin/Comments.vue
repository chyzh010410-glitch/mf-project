<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="对象类型">
          <el-select v-model="queryForm.targetType" placeholder="全部" clearable style="width:140px">
            <el-option label="百科" value="encyclopedia" />
            <el-option label="文章" value="article" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.isDeleted" placeholder="全部" clearable style="width:110px">
            <el-option label="正常" :value="0" />
            <el-option label="已屏蔽" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="搜索评论内容" clearable @keyup.enter="handleSearch" style="width:180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar"><span class="toolbar-title">评论管理</span></div>

    <el-table v-loading="loading" :data="tableData" border stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="targetType" label="类型" width="80" align="center">
        <template #default="{row}">
          <el-tag size="small">{{ row.targetType==='encyclopedia'?'百科':'文章' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetId" label="对象ID" width="80" align="center" />
      <el-table-column prop="userId" label="用户ID" width="80" align="center" />
      <el-table-column prop="content" label="评论内容" min-width="220" show-overflow-tooltip />
      <el-table-column prop="isDeletedByAdmin" label="状态" width="80" align="center">
        <template #default="{row}">
          <el-tag size="small" :type="row.isDeletedByAdmin===1?'danger':'success'">
            {{ row.isDeletedByAdmin===1?'已屏蔽':'正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="160" align="center">
        <template #default="{row}">{{ row.createTime?.substring(0,16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{row}">
          <el-button v-if="row.isDeletedByAdmin!==1" type="danger" link size="small" @click="handleHide(row)">屏蔽</el-button>
          <el-button v-else type="success" link size="small" @click="handleRestore(row)">恢复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10,20,50]"
      layout="total,sizes,prev,pager,next,jumper" background
      @current-change="fetchData" @size-change="fetchData"
    />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getCommentPage, hideComment, restoreComment } from '@/api/admin'

const queryForm = reactive({ targetType:'', isDeleted:null, keyword:'', page:1, size:10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryForm }
    if (params.isDeleted===null||params.isDeleted==='') delete params.isDeleted
    if (!params.targetType) delete params.targetType
    if (!params.keyword) delete params.keyword
    const res = await getCommentPage(params)
    if (res.code===200&&res.data){ tableData.value=res.data.records||[]; total.value=res.data.total||0 }
  } catch {} finally { loading.value=false }
}
const handleSearch = () => { queryForm.page=1; fetchData() }
const handleReset = () => { queryForm.targetType=''; queryForm.isDeleted=null; queryForm.keyword=''; queryForm.page=1; fetchData() }
const handleHide = async (row) => {
  try { await hideComment(row.id); row.isDeletedByAdmin=1; ElMessage.success('已屏蔽') } catch {}
}
const handleRestore = async (row) => {
  try { await restoreComment(row.id); row.isDeletedByAdmin=0; ElMessage.success('已恢复') } catch {}
}
onMounted(fetchData)
</script>
