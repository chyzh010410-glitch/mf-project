<template>
  <section class="page-grid">
    <div class="action-bar">
      <div>
        <h2>指标字典</h2>
        <p>统一维护指标编码、名称、来源、口径、周期和负责人。看板、快照和统一查询 API 都以这里的定义作为治理基准。</p>
      </div>
      <div class="action-buttons">
        <el-button :icon="RefreshCw" @click="refreshDaily">刷新日快照</el-button>
        <el-button type="success" :icon="Clock3" @click="refreshHourly">刷新小时快照</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增指标</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table :data="definitions" size="small" stripe>
        <el-table-column prop="metricCode" label="指标编码" width="180" />
        <el-table-column prop="metricName" label="指标名称" width="150" />
        <el-table-column prop="sourceTable" label="来源" min-width="220" show-overflow-tooltip />
        <el-table-column prop="formula" label="口径" min-width="260" show-overflow-tooltip />
        <el-table-column prop="period" label="周期" width="130" />
        <el-table-column prop="owner" label="负责人" width="130" />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Pencil" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.enabled ? 'warning' : 'success'" @click="toggleDefinition(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">指标计算注册表</h2>
          <p class="muted">快照刷新只执行已启用的受控计算项，计算口径在这里集中展示和启停。</p>
        </div>
        <el-tag effect="plain">{{ computeRegistries.length }} 项</el-tag>
      </div>
      <el-table :data="computeRegistries" size="small" stripe>
        <el-table-column prop="metricCode" label="指标编码" width="180" />
        <el-table-column prop="computeHandler" label="计算处理器" width="220" />
        <el-table-column prop="sourceName" label="来源" width="110" />
        <el-table-column prop="sourceContract" label="源契约" width="220" show-overflow-tooltip />
        <el-table-column prop="computeMode" label="模式" width="130" />
        <el-table-column prop="formulaText" label="口径" min-width="260" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link :type="row.enabled ? 'warning' : 'success'" @click="toggleComputeRegistry(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="two-col">
      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">统一指标查询</h2>
          <el-button :icon="Search" @click="loadQuery">查询</el-button>
        </div>
        <div class="toolbar metric-query-toolbar">
          <el-select v-model="query.codes" placeholder="选择指标" multiple collapse-tags collapse-tags-tooltip clearable>
            <el-option v-for="item in definitions" :key="item.metricCode" :label="item.metricName" :value="item.metricCode" />
          </el-select>
          <el-segmented v-model="query.period" :options="['daily', 'hourly']" />
          <el-input v-model="query.dimensionKey" placeholder="维度 key" clearable />
          <el-input v-model="query.dimensionValue" placeholder="维度值" clearable />
          <el-input-number v-model="query.limit" :min="1" :max="1000" controls-position="right" />
        </div>
        <el-table :data="queryRows" size="small" max-height="360">
          <el-table-column prop="metricCode" label="编码" width="170" />
          <el-table-column prop="metricName" label="名称" width="150" />
          <el-table-column prop="metricValue" label="值" width="120" />
          <el-table-column prop="dimensionKey" label="维度 key" width="110" />
          <el-table-column prop="dimensionValue" label="维度值" width="130" />
          <el-table-column prop="snapshotGranularity" label="粒度" width="100" />
          <el-table-column prop="snapshotTime" label="快照时间" min-width="180" />
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">最新快照</h2>
          <el-tag effect="plain">{{ latestRows.length }} 项</el-tag>
        </div>
        <div class="snapshot-list">
          <div v-for="item in latestRows" :key="item.metricCode" class="snapshot-row">
            <span>{{ item.metricName }}</span>
            <strong>{{ item.metricValue }}</strong>
            <code>{{ item.snapshotGranularity }} · {{ item.snapshotTime }}</code>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑指标' : '新增指标'" width="640px">
      <el-form label-width="90px">
        <div class="form-row">
          <el-form-item label="编码">
            <el-input v-model="form.metricCode" placeholder="gmv_total" />
          </el-form-item>
          <el-form-item label="名称">
            <el-input v-model="form.metricName" placeholder="GMV" />
          </el-form-item>
        </div>
        <el-form-item label="来源">
          <el-input v-model="form.sourceTable" placeholder="MF_EP.order" />
        </el-form-item>
        <el-form-item label="口径">
          <el-input v-model="form.formula" type="textarea" :rows="3" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="周期">
            <el-input v-model="form.period" placeholder="hourly,daily" />
          </el-form-item>
          <el-form-item label="负责人">
            <el-input v-model="form.owner" placeholder="platform-ops" />
          </el-form-item>
        </div>
        <el-form-item label="说明">
          <el-input v-model="form.description" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDefinition">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Clock3, Pencil, Plus, RefreshCw, Search } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const definitions = ref([])
const computeRegistries = ref([])
const latest = ref({})
const queryRows = ref([])
const query = reactive({
  codes: ['gmv_total', 'order_total'],
  period: 'daily',
  dimensionKey: '',
  dimensionValue: '',
  limit: 100
})
const latestRows = computed(() => Object.values(latest.value || {}))

const dialogVisible = ref(false)
const editingId = ref(null)
const form = reactive(emptyForm())

onMounted(load)

function emptyForm() {
  return {
    metricCode: '',
    metricName: '',
    sourceTable: '',
    formula: '',
    period: 'hourly,daily',
    owner: '',
    description: '',
    enabled: true
  }
}

async function load() {
  const [dict, registries, latestMetrics] = await Promise.all([
    api.metricDictionary(),
    api.metricComputeRegistry(),
    api.metricLatest()
  ])
  definitions.value = dict
  computeRegistries.value = registries
  latest.value = latestMetrics
  await loadQuery()
}

async function loadQuery() {
  queryRows.value = await api.metricQuery({
    code: query.codes.join(','),
    period: query.period,
    dimensionKey: query.dimensionKey || undefined,
    dimensionValue: query.dimensionValue || undefined,
    limit: query.limit
  })
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function saveDefinition() {
  if (editingId.value) {
    await api.updateMetricDefinition(editingId.value, form)
    ElMessage.success('指标已更新')
  } else {
    await api.createMetricDefinition(form)
    ElMessage.success('指标已新增')
  }
  dialogVisible.value = false
  await load()
}

async function toggleDefinition(row) {
  await api.setMetricDefinitionEnabled(row.id, !row.enabled)
  ElMessage.success(row.enabled ? '指标已停用' : '指标已启用')
  await load()
}

async function toggleComputeRegistry(row) {
  await api.setMetricComputeRegistryEnabled(row.id, !row.enabled)
  ElMessage.success(row.enabled ? '计算项已停用' : '计算项已启用')
  await load()
}

async function refreshDaily() {
  await api.refreshDailySnapshots()
  ElMessage.success('日快照已刷新')
  await load()
}

async function refreshHourly() {
  await api.refreshHourlySnapshots()
  ElMessage.success('小时快照已刷新')
  await load()
}
</script>
