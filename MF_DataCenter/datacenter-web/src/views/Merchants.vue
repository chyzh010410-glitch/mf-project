<template>
  <section class="page-grid">
    <div class="two-col">
      <div class="panel">
        <h2 class="panel-title">商家审核状态分布</h2>
        <ChartBox :option="statusOption" />
      </div>
      <div class="panel">
        <h2 class="panel-title">发货风险列表</h2>
        <el-table :data="data.shippingRisks" size="small">
          <el-table-column prop="name" label="商家" />
          <el-table-column prop="reason" label="风险" />
        </el-table>
      </div>
    </div>
    <div class="two-col">
      <div class="panel">
        <h2 class="panel-title">商家商品数排行</h2>
        <el-table :data="data.productRank" size="small">
          <el-table-column prop="name" label="商家" />
          <el-table-column prop="value" label="商品数" width="100" />
        </el-table>
      </div>
      <div class="panel">
        <h2 class="panel-title">商家订单项排行</h2>
        <el-table :data="data.orderItemRank" size="small">
          <el-table-column prop="name" label="商家" />
          <el-table-column prop="value" label="订单项" width="100" />
        </el-table>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { api } from '../api'
import ChartBox from '../components/ChartBox.vue'

const data = reactive({ statusDistribution: [], productRank: [], orderItemRank: [], shippingRisks: [] })

onMounted(async () => Object.assign(data, await api.merchants()))

const statusOption = computed(() => ({
  tooltip: { trigger: 'item' },
  color: ['#d0a85f', '#2f7d4d', '#d96d62', '#6d7e70'],
  series: [{ type: 'pie', radius: '70%', data: data.statusDistribution.map((item) => ({ name: item.status, value: item.count })) }]
}))
</script>
