<template>
  <section class="two-col">
    <div class="panel">
      <h2 class="panel-title">热门商品</h2>
      <el-table :data="data.hotProducts" size="small">
        <el-table-column prop="name" label="商品" />
        <el-table-column prop="value" label="热度" width="110" />
        <el-table-column prop="note" label="备注" />
      </el-table>
    </div>
    <div class="panel">
      <h2 class="panel-title">风险商品</h2>
      <el-table :data="data.riskProducts" size="small">
        <el-table-column prop="name" label="商品" />
        <el-table-column prop="reason" label="原因" />
        <el-table-column prop="type" label="类型" width="130" />
      </el-table>
    </div>
    <div class="panel">
      <h2 class="panel-title">分类销售排行</h2>
      <ChartBox :option="barOption(data.categorySales)" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { api } from '../api'
import ChartBox from '../components/ChartBox.vue'

const data = reactive({ hotProducts: [], riskProducts: [], categorySales: [] })

onMounted(async () => Object.assign(data, await api.products()))

function barOption(items) {
  return {
    grid: { left: 80, right: 24, top: 18, bottom: 28 },
    tooltip: {},
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: items.map((item) => item.name) },
    series: [{ type: 'bar', data: items.map((item) => item.value), itemStyle: { color: '#4f9e6c', borderRadius: 4 } }]
  }
}
</script>
