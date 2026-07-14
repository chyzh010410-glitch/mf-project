<template>
  <section class="page-grid">
    <div class="action-bar">
      <div><h2>治理通知中心</h2><p>聚合源表契约、快照、质量规则和 Agent 工具调用的真实风险信号。</p></div>
      <el-tag type="warning" effect="plain">{{ unread }} 条未读</el-tag>
    </div>
    <div class="panel">
      <el-table :data="rows" size="small" stripe>
        <el-table-column prop="title" label="通知" min-width="220" />
        <el-table-column prop="content" label="说明" min-width="260" />
        <el-table-column prop="severity" label="等级" width="100"><template #default="{ row }"><el-tag :type="tagType(row.severity)" effect="plain">{{ label(row.severity) }}</el-tag></template></el-table-column>
        <el-table-column prop="updateTime" label="最近发现" width="180" />
        <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="open(row)">{{ row.read ? '查看' : '查看并标记已读' }}</el-button></template></el-table-column>
      </el-table>
    </div>
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
const rows = ref([]); const unread = ref(0); const router = useRouter()
onMounted(load)
async function load () { rows.value = await api.notifications(); unread.value = rows.value.filter(item => !item.read).length }
async function open (row) { if (!row.read) await api.markNotificationRead(row.id); await router.push(row.targetPath || '/dashboard'); await load() }
function tagType (value) { return value === 'error' ? 'danger' : value === 'warning' ? 'warning' : 'success' }
function label (value) { return ({ error: '阻断', warning: '需关注', info: '提示' })[value] || value }
</script>
