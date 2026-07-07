<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词"><el-input v-model="queryForm.keyword" placeholder="搜索问题" clearable @keyup.enter="handleSearch" style="width:200px" /></el-form-item>
        <el-form-item label="分类"><el-select v-model="queryForm.category" placeholder="全部" clearable style="width:120px">
          <el-option label="注册登录" value="注册登录" /><el-option label="订单问题" value="订单问题" /><el-option label="商品售后" value="商品售后" /><el-option label="养护问题" value="养护问题" />
        </el-select></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button><el-button @click="handleReset">重置</el-button></el-form-item>
      </el-form>
    </div>
    <div class="toolbar"><span class="toolbar-title">FAQ管理</span><el-button type="primary" :icon="Plus" @click="handleAdd">新增FAQ</el-button></div>
    <el-table v-loading="loading" :data="tableData" border stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="100" align="center" />
      <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
      <el-table-column prop="isPublished" label="发布" width="80" align="center">
        <template #default="{row}"><el-tag size="small" :type="row.isPublished===1?'success':'info'">{{ row.isPublished===1?'是':'否' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{row}">
          <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
      :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper" background
      @current-change="fetchData" @size-change="fetchData" />
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑FAQ':'新增FAQ'" width="560px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="分类" prop="category"><el-select v-model="formData.category" placeholder="请选择" style="width:100%">
          <el-option label="注册登录" value="注册登录" /><el-option label="订单问题" value="订单问题" /><el-option label="商品售后" value="商品售后" /><el-option label="养护问题" value="养护问题" />
        </el-select></el-form-item>
        <el-form-item label="问题" prop="question"><el-input v-model="formData.question" placeholder="请输入问题" /></el-form-item>
        <el-form-item label="答案" prop="answer"><el-input v-model="formData.answer" type="textarea" :rows="4" placeholder="请输入答案" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="formData.sortOrder" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="发布"><el-switch v-model="formData.isPublished" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="handleSave">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'; import { ElMessage, ElMessageBox } from 'element-plus'; import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getFaqPage, getFaqDetail, saveFaq, updateFaq, deleteFaq } from '@/api/admin'
const queryForm=reactive({ keyword:'', category:'', page:1, size:10 })
const loading=ref(false); const tableData=ref([]); const total=ref(0)
const dialogVisible=ref(false); const isEdit=ref(false); const saving=ref(false); const formRef=ref(null); const editId=ref(null)
const df=()=>({ question:'', answer:'', category:'', sortOrder:0, isPublished:1 })
const formData=reactive(df())
const rules={ question:[{required:true,message:'请输入问题'}], answer:[{required:true,message:'请输入答案'}], category:[{required:true,message:'请选择分类'}] }
const fetchData=async()=>{ loading.value=true; try{ const p={...queryForm}; if(!p.keyword) delete p.keyword; if(!p.category) delete p.category
  const r=await getFaqPage(p); if(r.code===200&&r.data){ tableData.value=r.data.records||[]; total.value=r.data.total||0 } } catch{} finally{loading.value=false} }
const handleSearch=()=>{ queryForm.page=1; fetchData() }
const handleReset=()=>{ queryForm.keyword=''; queryForm.category=''; queryForm.page=1; fetchData() }
const handleAdd=()=>{ isEdit.value=false; editId.value=null; Object.assign(formData,df()); dialogVisible.value=true }
const handleEdit=async(row)=>{ isEdit.value=true; editId.value=row.id
  try{ const r=await getFaqDetail(row.id); if(r.code===200&&r.data) Object.assign(formData,{ question:r.data.question||'', answer:r.data.answer||'', category:r.data.category||'', sortOrder:r.data.sortOrder??0, isPublished:r.data.isPublished??0 }) } catch{}
  dialogVisible.value=true }
const handleSave=async()=>{ const v=await formRef.value.validate().catch(()=>false); if(!v) return; saving.value=true
  try{ isEdit.value?await updateFaq(editId.value,formData):await saveFaq(formData); ElMessage.success(isEdit.value?'更新成功':'新增成功'); dialogVisible.value=false; fetchData() } catch{} finally{saving.value=false} }
const handleDelete=(row)=>{ ElMessageBox.confirm(`确认删除「${row.question}」？`,'删除确认',{type:'warning'}).then(async()=>{ await deleteFaq(row.id); ElMessage.success('已删除'); fetchData() }).catch(()=>{}) }
onMounted(fetchData)
</script>
