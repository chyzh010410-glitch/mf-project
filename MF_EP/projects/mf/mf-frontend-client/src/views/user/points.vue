<template>
  <div class="page-container">
    <h2 class="section-title">我的积分</h2>
    <div class="points-card" v-loading="loading">
      <div class="points-num">{{ points }}</div>
      <div class="points-label">当前积分</div>
    </div>
    <h3 style="margin:24px 0 12px;font-size:15px;color:#333">积分明细</h3>
    <el-table :data="records" border stripe size="small" v-loading="loading">
      <el-table-column prop="id" label="序号" width="70" align="center" />
      <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
      <el-table-column label="变动" width="100" align="center">
        <template #default="{row}">
          <span :style="{color: row.points>0?'#2d8c4a':'#e74c3c'}">{{ row.points>0?'+':'' }}{{ row.points }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="balanceAfter" label="余额" width="100" align="center" />
      <el-table-column label="时间" width="170" align="center">
        <template #default="{row}">{{ row.createTime?.substring(0,19) || '-' }}</template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="total > size"
      v-model:current-page="page" :page-size="size" :total="total"
      :page-sizes="[10,20]" background layout="prev,pager,next" style="justify-content:center;margin-top:20px"
      @current-change="fetchRecords" @size-change="fetchRecords"
    />
    <el-empty v-if="!loading && records.length===0" description="暂无积分记录" style="margin-top:40px" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPoints, getPointsRecords } from '@/api/points'

const points = ref(0)
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await getPointsRecords({ page: page.value, size: size.value })
    if (res.code === 200 && res.data) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {} finally { loading.value = false }
}

onMounted(async () => {
  loading.value = true
  try {
    const [pr, rr] = await Promise.all([getPoints(), getPointsRecords({ page:1, size:10 })])
    if (pr.code === 200 && pr.data) points.value = pr.data.points || 0
    if (rr.code === 200 && rr.data) {
      records.value = rr.data.records || []
      total.value = rr.data.total || 0
    }
  } catch {} finally { loading.value = false }
})
</script>

<style scoped>
.points-card { background: linear-gradient(135deg, #2d8c4a, #3cb060); border-radius: 12px; padding: 32px; text-align: center; color: #fff; }
.points-num { font-size: 48px; font-weight: 700; }
.points-label { font-size: 14px; opacity: .85; margin-top: 8px; }
</style>
