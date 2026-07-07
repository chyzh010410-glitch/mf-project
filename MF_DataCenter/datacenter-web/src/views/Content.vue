<template>
  <section class="page-grid">
    <div class="two-col">
      <div class="panel">
        <h2 class="panel-title">热门百科</h2>
        <el-table :data="data.hotEncyclopedias" size="small">
          <el-table-column prop="name" label="标题" />
          <el-table-column prop="value" label="浏览" width="100" />
        </el-table>
      </div>
      <div class="panel">
        <h2 class="panel-title">热门文章</h2>
        <el-table :data="data.hotArticles" size="small">
          <el-table-column prop="name" label="标题" />
          <el-table-column prop="value" label="浏览" width="100" />
        </el-table>
      </div>
    </div>
    <div class="panel">
      <h2 class="panel-title">评论/收藏/点赞趋势</h2>
      <ChartBox :option="trendOption" />
    </div>
    <div class="panel">
      <h2 class="panel-title">内容知识缺口</h2>
      <el-tag v-for="item in data.knowledgeGaps" :key="item" type="success" effect="plain" style="margin: 8px 8px 0 0">{{ item }}</el-tag>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { api } from '../api'
import ChartBox from '../components/ChartBox.vue'

const data = reactive({ hotEncyclopedias: [], hotArticles: [], interactionTrend: [], knowledgeGaps: [] })

onMounted(async () => Object.assign(data, await api.content()))

const trendOption = computed(() => ({
  grid: { left: 40, right: 20, top: 24, bottom: 32 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: data.interactionTrend.map((item) => item.date) },
  yAxis: { type: 'value' },
  series: [{ type: 'line', smooth: true, data: data.interactionTrend.map((item) => item.value), itemStyle: { color: '#2f7d4d' } }]
}))
</script>
