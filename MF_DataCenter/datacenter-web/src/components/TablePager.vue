<template>
  <div class="table-pager"><el-pagination :current-page="pageNo" :page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="changePage" @size-change="changeSize" /></div>
</template>
<script setup>
const props = defineProps({ pageNo: Number, pageSize: Number, total: Number })
const emit = defineEmits(['change'])
function changePage (pageNo) { emit('change', { pageNo, pageSize: props.pageSize }) }
function changeSize (pageSize) { emit('change', { pageNo: 1, pageSize }) }
</script>
