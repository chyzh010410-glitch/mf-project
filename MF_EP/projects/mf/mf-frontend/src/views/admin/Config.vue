<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :model="q" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="q.keyword"
            placeholder="搜索配置键"
            clearable
            style="width: 180px"
            @keyup.enter="doSearch"
          />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="q.configGroup" placeholder="全部分组" clearable style="width: 130px">
            <el-option label="通用" value="general" />
            <el-option label="支付" value="payment" />
            <el-option label="积分" value="points" />
            <el-option label="活动" value="activity" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">搜索</el-button>
          <el-button @click="doReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar">
      <span class="toolbar-title">平台设置</span>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增配置</el-button>
    </div>

    <el-alert
      class="config-tips"
      type="info"
      :closable="false"
      show-icon
      title="核心配置会影响 C 端展示：导航文案、首页数量、活动 Banner 和支付开关。修改后请刷新 C 端页面验收。"
    />

    <el-table v-loading="loading" :data="table" border stripe>
      <el-table-column prop="id" label="ID" width="65" align="center" />
      <el-table-column prop="configKey" label="配置键" width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag v-if="configMeta(row.configKey)" size="small" type="success" effect="plain">核心</el-tag>
          <span class="config-key">{{ row.configKey }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="configValue" label="配置值" min-width="180" show-overflow-tooltip />
      <el-table-column prop="configGroup" label="分组" width="90" align="center">
        <template #default="{ row }">{{ groupLabel(row.configGroup) }}</template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.description || configMeta(row.configKey)?.tip || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="q.page"
      v-model:page-size="q.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @current-change="fetch"
      @size-change="fetch"
    />

    <el-dialog
      v-model="vis"
      :title="edit ? '编辑配置' : '新增配置'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="fr" :model="f" :rules="rls" label-width="90px">
        <el-form-item label="配置键" prop="configKey">
          <el-select
            v-model="f.configKey"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入配置键"
            style="width: 100%"
            @change="handleConfigKeyChange"
          >
            <el-option
              v-for="item in knownConfigs"
              :key="item.key"
              :label="`${item.key} - ${item.name}`"
              :value="item.key"
            />
          </el-select>
        </el-form-item>

        <el-alert
          v-if="activeMeta"
          class="dialog-tip"
          type="info"
          :closable="false"
          :title="activeMeta.tip"
        />

        <el-form-item label="配置值" prop="configValue">
          <el-switch
            v-if="activeMeta?.type === 'boolean'"
            :model-value="toBoolean(f.configValue)"
            active-text="开启"
            inactive-text="关闭"
            @change="setBooleanValue"
          />
          <el-input-number
            v-else-if="activeMeta?.type === 'limit'"
            :model-value="toNumber(f.configValue, activeMeta.defaultValue)"
            :min="1"
            :max="20"
            style="width: 180px"
            @change="setNumberValue"
          />
          <el-input
            v-else
            v-model="f.configValue"
            type="textarea"
            :rows="3"
            placeholder="配置值"
          />
        </el-form-item>

        <el-form-item label="分组">
          <el-select v-model="f.configGroup" placeholder="请选择" style="width: 100%">
            <el-option label="通用" value="general" />
            <el-option label="支付" value="payment" />
            <el-option label="积分" value="points" />
            <el-option label="活动" value="activity" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="f.description" placeholder="配置说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getConfigPage, getConfigDetail, saveConfig, updateConfig, deleteConfig } from '@/api/admin'

const knownConfigs = [
  { key: 'nav_product_label', name: '商品导航名称', type: 'text', group: 'general', tip: '影响 C 端顶部导航中商品入口的文案。' },
  { key: 'nav_encyclopedia_label', name: '百科导航名称', type: 'text', group: 'general', tip: '影响 C 端顶部导航中百科入口的文案。' },
  { key: 'activity_banner_enabled', name: '首页活动 Banner', type: 'boolean', group: 'activity', tip: '关闭后 C 端首页隐藏活动 Banner。' },
  { key: 'home_recommend_product_limit', name: '首页推荐商品数量', type: 'limit', group: 'general', defaultValue: 8, tip: '控制 C 端首页推荐商品展示数量，建议 1-20。' },
  { key: 'home_new_product_limit', name: '首页新品数量', type: 'limit', group: 'general', defaultValue: 8, tip: '控制 C 端首页新品商品展示数量，建议 1-20。' },
  { key: 'home_recommend_article_limit', name: '首页推荐文章数量', type: 'limit', group: 'general', defaultValue: 4, tip: '控制 C 端首页推荐文章展示数量，建议 1-20。' },
  { key: 'payment_enabled', name: '支付开关', type: 'boolean', group: 'payment', tip: '关闭后 C 端支付页提示支付暂未开放，开启后允许模拟支付。' }
]

const q = reactive({ keyword: '', configGroup: '', page: 1, size: 10 })
const loading = ref(false)
const table = ref([])
const total = ref(0)
const vis = ref(false)
const edit = ref(false)
const saving = ref(false)
const fr = ref(null)
const eid = ref(null)

const df = () => ({ configKey: '', configValue: '', configGroup: 'general', description: '' })
const f = reactive(df())
const rls = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'change' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'change' }]
}

const activeMeta = computed(() => configMeta(f.configKey))

const configMeta = (key) => knownConfigs.find(item => item.key === key)
const groupLabel = (group) => ({ general: '通用', payment: '支付', points: '积分', activity: '活动' }[group] || group || '-')
const toBoolean = (value) => value === true || value === 'true' || value === '1'
const toNumber = (value, fallback = 1) => {
  const number = Number(value)
  return Number.isFinite(number) && number > 0 ? number : fallback
}
const setBooleanValue = (value) => { f.configValue = value ? 'true' : 'false' }
const setNumberValue = (value) => { f.configValue = String(value || '') }

const handleConfigKeyChange = () => {
  const meta = activeMeta.value
  if (!meta) return
  f.configGroup = meta.group
  if (!f.description) f.description = meta.tip
  if (!f.configValue) {
    if (meta.type === 'boolean') f.configValue = 'false'
    if (meta.type === 'limit') f.configValue = String(meta.defaultValue || 8)
  }
}

const fetch = async () => {
  loading.value = true
  try {
    const params = { ...q }
    if (!params.keyword) delete params.keyword
    if (!params.configGroup) delete params.configGroup
    const res = await getConfigPage(params)
    if (res.code === 200 && res.data) {
      table.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch { /* ignore */ } finally { loading.value = false }
}

const doSearch = () => { q.page = 1; fetch() }
const doReset = () => { q.keyword = ''; q.configGroup = ''; q.page = 1; fetch() }

const handleAdd = () => {
  edit.value = false
  eid.value = null
  Object.assign(f, df())
  vis.value = true
}

const handleEdit = async (row) => {
  edit.value = true
  eid.value = row.id
  try {
    const res = await getConfigDetail(row.id)
    if (res.code === 200 && res.data) {
      Object.assign(f, {
        configKey: res.data.configKey || '',
        configValue: res.data.configValue || '',
        configGroup: res.data.configGroup || 'general',
        description: res.data.description || ''
      })
      handleConfigKeyChange()
    }
  } catch { /* ignore */ }
  vis.value = true
}

const handleSave = async () => {
  const valid = await fr.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    edit.value ? await updateConfig(eid.value, f) : await saveConfig(f)
    ElMessage.success(edit.value ? '更新成功' : '新增成功')
    vis.value = false
    fetch()
  } catch { /* ignore */ } finally { saving.value = false }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除「${row.configKey}」？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteConfig(row.id)
      ElMessage.success('已删除')
      fetch()
    })
    .catch(() => {})
}

onMounted(fetch)
</script>

<style scoped>
.config-tips {
  margin-bottom: 12px;
}

.dialog-tip {
  margin-bottom: 16px;
}

.config-key {
  margin-left: 6px;
}
</style>
