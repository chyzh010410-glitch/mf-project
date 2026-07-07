<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="搜索名称" clearable @keyup.enter="handleSearch" style="width:160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar"><span class="toolbar-title">用户上传审核</span></div>

    <el-table v-loading="loading" :data="tableData" border stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="userId" label="用户ID" width="80" align="center" />
      <el-table-column prop="name" label="名称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="location" label="地点" width="100" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
      <el-table-column prop="images" label="图片" width="100" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{row}">
          <el-tag size="small" :type="row.status==='approved'?'success':row.status==='rejected'?'danger':'warning'">
            {{ row.status==='approved'?'已通过':row.status==='rejected'?'已驳回':'待审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" width="160" align="center">
        <template #default="{row}">{{ row.createTime?.substring(0,16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{row}">
          <template v-if="row.status==='pending'">
            <el-button type="success" link size="small" @click="handleReview(row,'approved')">通过</el-button>
            <el-button type="danger" link size="small" @click="handleReject(row)">驳回</el-button>
          </template>
          <span v-else style="color:#bbb">{{ row.reviewComment||'已审核' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10,20,50]"
      layout="total,sizes,prev,pager,next,jumper" background
      @current-change="fetchData" @size-change="fetchData"
    />

    <el-dialog v-model="rejectVisible" title="驳回原因" width="400px" :close-on-click-modal="false">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
      <template #footer>
        <el-button @click="rejectVisible=false">取消</el-button>
        <el-button type="primary" @click="doReject">确定驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getUploadPage, reviewUpload } from '@/api/admin'

const queryForm = reactive({ status:'', keyword:'', page:1, size:10 })
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectId = ref(null)

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryForm }
    if (!params.status) delete params.status
    if (!params.keyword) delete params.keyword
    const res = await getUploadPage(params)
    if (res.code===200&&res.data){ tableData.value=res.data.records||[]; total.value=res.data.total||0 }
  } catch {} finally { loading.value=false }
}
const handleSearch = () => { queryForm.page=1; fetchData() }
const handleReset = () => { queryForm.status=''; queryForm.keyword=''; queryForm.page=1; fetchData() }
const handleReview = async (row, status) => {
  try { await reviewUpload(row.id,{status,reviewComment:''}); row.status=status; ElMessage.success(status==='approved'?'已通过':'已驳回') } catch {}
}
const handleReject = (row) => { rejectId.value=row.id; rejectReason.value=''; rejectVisible.value=true }
const doReject = async () => {
  try {
    await reviewUpload(rejectId.value,{status:'rejected',reviewComment:rejectReason.value})
    const row = tableData.value.find(r=>r.id===rejectId.value)
    if (row) { row.status='rejected'; row.reviewComment=rejectReason.value }
    rejectVisible.value=false; ElMessage.success('已驳回')
  } catch {}
}
onMounted(fetchData)
</script>
