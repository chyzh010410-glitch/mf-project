<template>
  <section class="page-grid">
    <div class="quality-summary">
      <div class="summary-item">
        <span>源库连接</span>
        <strong>{{ check.connected ? '正常' : '异常' }}</strong>
      </div>
      <div class="summary-item">
        <span>契约表</span>
        <strong>{{ check.tableTotal }}</strong>
      </div>
      <div class="summary-item warning">
        <span>异常表</span>
        <strong>{{ check.failedTables }}</strong>
      </div>
      <div class="summary-item danger">
        <span>缺失字段</span>
        <strong>{{ check.missingFields }}</strong>
      </div>
    </div>

    <div class="action-bar">
      <div>
        <h2>源表契约</h2>
        <p>把 DataCenter 依赖的 MF_EP 表和字段显式登记，并在快照、指标、质量检查之前先确认源库结构是否满足契约。</p>
      </div>
      <div class="action-buttons">
        <el-button type="success" :icon="RefreshCw" @click="runCheck">立即检查</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">检查结果</h2>
          <p class="muted">{{ check.message || '尚未检查' }}</p>
        </div>
        <el-tag :type="check.connected && check.failedTables === 0 ? 'success' : 'warning'" effect="plain">
          {{ check.checkedAt || '-' }}
        </el-tag>
      </div>
      <el-table :data="check.tables" size="small" stripe>
        <el-table-column prop="tableName" label="源表" width="150" />
        <el-table-column prop="businessName" label="业务含义" width="170" />
        <el-table-column prop="tableExists" label="表存在" width="100">
          <template #default="{ row }">
            <el-tag :type="row.tableExists ? 'success' : 'danger'" effect="plain">{{ row.tableExists ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="passed" label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : 'danger'" effect="plain">{{ row.passed ? '通过' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="180" />
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-table :data="row.fields" size="small">
              <el-table-column prop="fieldName" label="字段" width="180" />
              <el-table-column prop="expectedType" label="期望类型" width="140" />
              <el-table-column prop="actualType" label="实际类型" width="140" />
              <el-table-column prop="required" label="必需" width="90">
                <template #default="{ row: field }">{{ field.required ? '是' : '否' }}</template>
              </el-table-column>
              <el-table-column prop="passed" label="结果" width="100">
                <template #default="{ row: field }">
                  <el-tag :type="field.passed ? 'success' : 'danger'" effect="plain">{{ field.passed ? '通过' : '失败' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="message" label="说明" />
            </el-table>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h2 class="panel-title">契约清单</h2>
        <el-tag effect="plain">{{ contracts.length }} 张表</el-tag>
      </div>
      <el-table :data="contracts" size="small" stripe>
        <el-table-column prop="sourceName" label="来源" width="100" />
        <el-table-column prop="schemaName" label="库名" width="130" />
        <el-table-column prop="tableName" label="表名" width="150" />
        <el-table-column prop="businessName" label="业务含义" width="170" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column label="字段数" width="90">
          <template #default="{ row }">{{ row.fields.length }}</template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const contracts = ref([])
const check = reactive({
  connected: false,
  message: '',
  checkedAt: '',
  tableTotal: 0,
  failedTables: 0,
  missingFields: 0,
  tables: []
})

onMounted(load)

async function load() {
  const [contractRows, checkResult] = await Promise.all([api.sourceContracts(), api.sourceCheck()])
  contracts.value = contractRows
  Object.assign(check, checkResult)
}

async function runCheck() {
  const result = await api.runSourceCheck()
  Object.assign(check, result)
  ElMessage.success('源库契约检查已完成')
}
</script>
