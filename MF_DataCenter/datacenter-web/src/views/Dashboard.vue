<template>
  <section class="page-grid">
    <div class="metric-grid">
      <div v-for="card in data.cards" :key="card.code" class="metric-card">
        <span>{{ card.name }}</span>
        <strong>{{ card.value }}</strong>
        <p>{{ card.note }}</p>
      </div>
    </div>

    <div class="status-band">
      <div class="status-cell">
        <span>服务状态</span>
        <strong>{{ status.status || 'loading' }}</strong>
      </div>
      <div class="status-cell">
        <span>源库契约</span>
        <strong>{{ data.governance?.sourceFailedTables ? `${data.governance.sourceFailedTables} 表异常` : '通过' }}</strong>
      </div>
      <div class="status-cell">
        <span>质量问题</span>
        <strong>{{ data.governance?.activeIssues ? `${data.governance.activeIssues} 个待处理` : '无待处理' }}</strong>
      </div>
      <div class="status-cell">
        <span>快照新鲜度</span>
        <strong>{{ data.governance?.snapshotAgeMinutes == null ? '-' : `${data.governance.snapshotAgeMinutes} 分钟` }}</strong>
      </div>
      <div class="status-cell">
        <span>最近 Agent 写入</span>
        <strong>{{ data.latestAgentWrite?.createTime || '暂无记录' }}</strong>
      </div>
    </div>

    <div class="action-bar">
      <div>
        <h2>{{ data.governance?.status === 'trusted' ? '数据可信' : '存在风险' }}</h2>
        <p>{{ data.governance?.message || '正在加载治理状态' }}</p>
      </div>
      <el-tag :type="data.governance?.status === 'trusted' ? 'success' : 'warning'" effect="plain">
        {{ data.governance?.latestSnapshotTime || '-' }}
      </el-tag>
    </div>

    <div class="two-col">
      <div v-if="data.governance?.status === 'risk'" class="panel">
        <div class="panel-header">
        <div>
          <h2 class="panel-title">治理风险原因</h2>
          <p v-for="reason in data.governance.riskReasons" :key="reason">{{ reason }}</p>
        </div>
        <el-button type="success" :loading="refreshing" @click="refreshSnapshots">刷新小时快照</el-button>
        </div>
      </div>

      <div class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">治理待办</h2>
          <p v-if="!data.governanceTasks?.length">当前没有待处理的治理任务</p>
        </div>
      </div>
      <div v-for="task in data.governanceTasks" :key="task.id" class="task-row">
        <div><strong>{{ task.title }}</strong><p>{{ task.content }} · {{ task.lastSeenTime }}</p></div>
        <el-button link type="primary" @click="$router.push(task.targetPath)">处理</el-button>
      </div>
      </div>
    </div>

    <div v-if="data.latestAgentWrite" class="panel">
      <div class="panel-header"><div><h2 class="panel-title">最近一次 Agent 写入</h2><p>{{ data.latestAgentWrite.question }}</p></div><el-tag effect="plain">{{ data.latestAgentWrite.source }} · {{ data.latestAgentWrite.intent || '未标注意图' }}</el-tag></div>
    </div>

    <div class="two-col">
      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">近 7 日订单趋势</h2>
          <el-tag effect="plain">daily snapshot</el-tag>
        </div>
        <ChartBox :option="lineOption(data.orderTrend, '#2f7d4d')" />
      </div>
      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">近 7 日 GMV 趋势</h2>
          <el-tag effect="plain">daily snapshot</el-tag>
        </div>
        <ChartBox :option="lineOption(data.gmvTrend, '#4f9e6c')" />
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h2 class="panel-title">分类销售额</h2>
        <el-tag effect="plain">category snapshot</el-tag>
      </div>
      <ChartBox :option="pieOption(data.categorySales)" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import ChartBox from '../components/ChartBox.vue'

const data = reactive({ cards: [], orderTrend: [], gmvTrend: [], categorySales: [], governance: null, latestAgentWrite: null, governanceTasks: [] })
const status = reactive({ status: '', aiStorage: null, mfEpDatasource: null })
const refreshing = ref(false)

onMounted(load)

async function load() {
  const [dashboard, systemStatus] = await Promise.all([
    api.dashboard(),
    api.systemStatus()
  ])
  Object.assign(data, dashboard)
  Object.assign(status, systemStatus)
}

async function refreshSnapshots() {
  refreshing.value = true
  try {
    await api.refreshHourlySnapshots()
    await load()
    ElMessage.success('小时快照已刷新')
  } finally {
    refreshing.value = false
  }
}

function lineOption(points, color) {
  return {
    grid: { left: 46, right: 18, top: 20, bottom: 32 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: points.map((item) => item.date), axisTick: { show: false } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#d9e8dc' } } },
    series: [{ type: 'line', smooth: true, data: points.map((item) => item.value), areaStyle: { opacity: 0.1 }, itemStyle: { color }, lineStyle: { color, width: 2 } }]
  }
}

function pieOption(items) {
  return {
    tooltip: { trigger: 'item' },
    color: ['#2f7d4d', '#6bab72', '#9ccf8d', '#d0a85f', '#8fb7c9', '#b48772'],
    series: [{ type: 'pie', radius: ['46%', '70%'], data: items.map((item) => ({ name: item.name, value: item.value })) }]
  }
}
</script>
