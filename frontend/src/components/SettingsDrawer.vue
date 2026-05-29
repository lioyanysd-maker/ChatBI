<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  createDataSource,
  deleteDataSource,
  testDataSourceConnection,
  updateDataSource,
} from '../api/datasource'
import { fetchLlmSettings, testLlmSettings, updateLlmSettings } from '../api/settings'
import type { DataSource, DataSourceForm } from '../types/datasource'
import type { LlmSettingsForm, SettingsTab } from '../types/settings'

const props = defineProps<{
  visible: boolean
  datasources: DataSource[]
  editing?: DataSource | null
  initialTab?: SettingsTab
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const activeTab = ref<SettingsTab>('llm')
const llmLoading = ref(false)
const llmTesting = ref(false)
const llmSaving = ref(false)
const dbTesting = ref(false)
const dbSaving = ref(false)
const llmConfigured = ref(false)
const llmKeyMasked = ref('')
const editingDbId = ref<number | null>(null)

const llmForm = reactive<LlmSettingsForm>({
  provider: 'deepseek',
  baseUrl: 'https://api.deepseek.com/v1',
  model: 'deepseek-chat',
  apiKey: '',
})

const dbForm = reactive<DataSourceForm>({
  name: '',
  dbType: 'mysql',
  host: '127.0.0.1',
  port: 3306,
  databaseName: '',
  username: 'root',
  password: '',
})

const isEditingDb = computed(() => editingDbId.value != null)

const providerPresets: Record<string, { baseUrl: string; model: string }> = {
  deepseek: {
    baseUrl: 'https://api.deepseek.com/v1',
    model: 'deepseek-chat',
  },
  openai: {
    baseUrl: 'https://api.openai.com/v1',
    model: 'gpt-4o-mini',
  },
  qwen: {
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    model: 'qwen-plus',
  },
}

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) return
    activeTab.value = props.initialTab ?? 'llm'
    await loadLlmSettings()
    resetDbForm()
  },
)

watch(
  () => props.editing,
  () => {
    if (props.visible) {
      resetDbForm()
    }
  },
)

watch(
  () => llmForm.provider,
  (provider) => {
    if (llmLoading.value) return
    const preset = providerPresets[provider]
    if (preset) {
      llmForm.baseUrl = preset.baseUrl
      llmForm.model = preset.model
    }
  },
)

watch(
  () => dbForm.dbType,
  (type) => {
    if (type === 'mysql' && dbForm.port === 5432) dbForm.port = 3306
    if ((type === 'pg' || type === 'postgresql') && dbForm.port === 3306) dbForm.port = 5432
  },
)

function close() {
  emit('update:visible', false)
}

async function loadLlmSettings() {
  llmLoading.value = true
  try {
    const settings = await fetchLlmSettings()
    llmForm.provider = settings.provider
    llmForm.baseUrl = settings.baseUrl
    llmForm.model = settings.model
    llmForm.apiKey = ''
    llmConfigured.value = settings.apiKeyConfigured
    llmKeyMasked.value = settings.apiKeyMasked
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载大模型配置失败')
  } finally {
    llmLoading.value = false
  }
}

function resetDbForm() {
  editingDbId.value = props.editing?.id ?? null
  if (props.editing) {
    dbForm.name = props.editing.name
    dbForm.dbType = props.editing.dbType
    dbForm.host = props.editing.host
    dbForm.port = props.editing.port
    dbForm.databaseName = props.editing.databaseName
    dbForm.username = props.editing.username
    dbForm.password = ''
    activeTab.value = 'database'
    return
  }
  dbForm.name = ''
  dbForm.dbType = 'mysql'
  dbForm.host = '127.0.0.1'
  dbForm.port = 3306
  dbForm.databaseName = ''
  dbForm.username = 'root'
  dbForm.password = ''
  editingDbId.value = null
}

async function handleTestLlm() {
  if (!llmForm.apiKey && !llmConfigured.value) {
    ElMessage.warning('请先填写 API Key')
    return
  }
  llmTesting.value = true
  try {
    await testLlmSettings({ ...llmForm })
    ElMessage.success('连接成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '连接失败')
  } finally {
    llmTesting.value = false
  }
}

async function handleSaveLlm() {
  if (!llmForm.provider || !llmForm.baseUrl || !llmForm.model) {
    ElMessage.warning('请填写完整配置')
    return
  }
  if (!llmForm.apiKey && !llmConfigured.value) {
    ElMessage.warning('请填写 API Key')
    return
  }
  llmSaving.value = true
  try {
    const settings = await updateLlmSettings({ ...llmForm })
    llmConfigured.value = settings.apiKeyConfigured
    llmKeyMasked.value = settings.apiKeyMasked
    llmForm.apiKey = ''
    ElMessage.success('大模型配置已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    llmSaving.value = false
  }
}

async function handleTestDb() {
  if (!dbForm.password) {
    ElMessage.warning('请先填写密码')
    return
  }
  dbTesting.value = true
  try {
    await testDataSourceConnection({ ...dbForm })
    ElMessage.success('连接成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '连接失败')
  } finally {
    dbTesting.value = false
  }
}

async function handleSaveDb() {
  if (!dbForm.name || !dbForm.host || !dbForm.databaseName || !dbForm.username) {
    ElMessage.warning('请填写完整配置')
    return
  }
  if (!dbForm.password && !isEditingDb.value) {
    ElMessage.warning('请填写密码')
    return
  }
  if (isEditingDb.value && !dbForm.password) {
    ElMessage.warning('更新配置需重新填写密码')
    return
  }

  dbSaving.value = true
  try {
    if (isEditingDb.value && editingDbId.value != null) {
      await updateDataSource(editingDbId.value, { ...dbForm })
      ElMessage.success('数据源已更新')
    } else {
      await createDataSource({ ...dbForm })
      ElMessage.success('数据源已添加')
    }
    emit('saved')
    editingDbId.value = null
    resetDbForm()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    dbSaving.value = false
  }
}

async function handleDeleteDb(item: DataSource) {
  try {
    await deleteDataSource(item.id)
    ElMessage.success('已删除')
    emit('saved')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function editDb(item: DataSource) {
  editingDbId.value = item.id
  dbForm.name = item.name
  dbForm.dbType = item.dbType
  dbForm.host = item.host
  dbForm.port = item.port
  dbForm.databaseName = item.databaseName
  dbForm.username = item.username
  dbForm.password = ''
}
</script>

<template>
  <Teleport to="body">
    <Transition name="settings-overlay">
      <div v-if="visible" class="settings-overlay" @click.self="close">
        <Transition name="settings-panel">
          <aside v-if="visible" class="settings-panel">
            <header class="panel-header">
              <h2 class="panel-title">设置</h2>
              <button class="close-btn" title="关闭" @click="close">
                <el-icon :size="18"><Close /></el-icon>
              </button>
            </header>

            <div class="panel-tabs">
              <button
                class="tab-btn"
                :class="{ active: activeTab === 'llm' }"
                @click="activeTab = 'llm'"
              >
                大模型
              </button>
              <button
                class="tab-btn"
                :class="{ active: activeTab === 'database' }"
                @click="activeTab = 'database'"
              >
                数据库
              </button>
            </div>

            <div class="panel-body">
              <div v-if="activeTab === 'llm'" v-loading="llmLoading" class="tab-content">
                <p class="section-desc">配置自然语言理解与 SQL 生成所使用的大模型 API。</p>
                <el-form label-position="top" class="settings-form" @submit.prevent>
                  <el-form-item label="服务商">
                    <el-select v-model="llmForm.provider" style="width: 100%">
                      <el-option label="DeepSeek" value="deepseek" />
                      <el-option label="OpenAI 兼容" value="openai" />
                      <el-option label="通义千问（兼容模式）" value="qwen" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="API 地址">
                    <el-input v-model="llmForm.baseUrl" placeholder="https://api.deepseek.com/v1" />
                  </el-form-item>
                  <el-form-item label="模型">
                    <el-input v-model="llmForm.model" placeholder="deepseek-chat" />
                  </el-form-item>
                  <el-form-item label="API Key">
                    <el-input
                      v-model="llmForm.apiKey"
                      type="password"
                      show-password
                      :placeholder="llmConfigured ? `已配置 ${llmKeyMasked}，留空则不修改` : '请输入 API Key'"
                    />
                  </el-form-item>
                </el-form>
                <div class="form-actions">
                  <button class="ds-btn ghost" :disabled="llmTesting" @click="handleTestLlm">
                    {{ llmTesting ? '测试中...' : '测试连接' }}
                  </button>
                  <button class="ds-btn primary" :disabled="llmSaving" @click="handleSaveLlm">
                    {{ llmSaving ? '保存中...' : '保存' }}
                  </button>
                </div>
              </div>

              <div v-else class="tab-content">
                <p class="section-desc">添加或编辑业务数据库连接，供 ChatBI 查询分析。</p>

                <div v-if="datasources.length" class="ds-list">
                  <div v-for="item in datasources" :key="item.id" class="ds-item">
                    <div class="ds-item-main">
                      <div class="ds-item-name">{{ item.name }}</div>
                      <div class="ds-item-meta">{{ item.dbType }} · {{ item.databaseName }}</div>
                    </div>
                    <div class="ds-item-actions">
                      <button class="link-btn" @click="editDb(item)">编辑</button>
                      <button class="link-btn danger" @click="handleDeleteDb(item)">删除</button>
                    </div>
                  </div>
                </div>

                <div class="form-divider">{{ isEditingDb ? '编辑连接' : '添加连接' }}</div>

                <el-form label-position="top" class="settings-form" @submit.prevent>
                  <el-form-item label="名称">
                    <el-input v-model="dbForm.name" placeholder="例如：业务库" />
                  </el-form-item>
                  <el-form-item label="类型">
                    <el-select v-model="dbForm.dbType" style="width: 100%">
                      <el-option label="MySQL" value="mysql" />
                      <el-option label="PostgreSQL" value="postgresql" />
                    </el-select>
                  </el-form-item>
                  <div class="form-row">
                    <el-form-item label="主机" class="flex-2">
                      <el-input v-model="dbForm.host" placeholder="127.0.0.1" />
                    </el-form-item>
                    <el-form-item label="端口" class="flex-1">
                      <el-input-number v-model="dbForm.port" :min="1" :max="65535" style="width: 100%" />
                    </el-form-item>
                  </div>
                  <el-form-item label="数据库">
                    <el-input v-model="dbForm.databaseName" placeholder="chatbi" />
                  </el-form-item>
                  <el-form-item label="用户名">
                    <el-input v-model="dbForm.username" />
                  </el-form-item>
                  <el-form-item label="密码">
                    <el-input v-model="dbForm.password" type="password" show-password placeholder="请输入密码" />
                  </el-form-item>
                </el-form>
                <div class="form-actions">
                  <button class="ds-btn ghost" :disabled="dbTesting" @click="handleTestDb">
                    {{ dbTesting ? '测试中...' : '测试连接' }}
                  </button>
                  <button class="ds-btn primary" :disabled="dbSaving" @click="handleSaveDb">
                    {{ dbSaving ? '保存中...' : '保存' }}
                  </button>
                </div>
              </div>
            </div>
          </aside>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.settings-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.28);
  display: flex;
  justify-content: flex-end;
}

.settings-panel {
  width: min(440px, 100vw);
  height: 100%;
  background: var(--ds-surface);
  border-left: 1px solid var(--ds-border);
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(15, 23, 42, 0.08);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 12px;
  flex-shrink: 0;
}

.panel-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--ds-text);
}

.close-btn {
  width: 34px;
  height: 34px;
  border: 1px solid var(--ds-border);
  border-radius: 10px;
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.close-btn:hover {
  border-color: var(--ds-accent);
  color: var(--ds-accent);
}

.panel-tabs {
  display: flex;
  gap: 8px;
  padding: 0 20px 12px;
  flex-shrink: 0;
}

.tab-btn {
  flex: 1;
  height: 36px;
  border: 1px solid var(--ds-border);
  border-radius: 10px;
  background: var(--ds-input-bg);
  color: var(--ds-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.tab-btn.active {
  background: rgba(77, 107, 254, 0.08);
  border-color: rgba(77, 107, 254, 0.35);
  color: var(--ds-accent);
  font-weight: 600;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px 24px;
}

.tab-content {
  min-height: 200px;
}

.section-desc {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ds-text-secondary);
}

.settings-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: var(--ds-text-secondary);
  padding-bottom: 4px;
}

.form-row {
  display: flex;
  gap: 12px;
}

.flex-1 { flex: 1; }
.flex-2 { flex: 2; }

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
}

.ds-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 10px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text);
  transition: all 0.15s;
}

.ds-btn.ghost:hover:not(:disabled) {
  border-color: var(--ds-accent);
  color: var(--ds-accent);
}

.ds-btn.primary {
  background: var(--ds-accent);
  border-color: var(--ds-accent);
  color: #fff;
}

.ds-btn.primary:hover:not(:disabled) {
  background: var(--ds-accent-hover);
}

.ds-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ds-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.ds-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--ds-border);
  border-radius: 12px;
  background: var(--ds-input-bg);
}

.ds-item-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-text);
}

.ds-item-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ds-text-secondary);
}

.ds-item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.link-btn {
  border: none;
  background: transparent;
  color: var(--ds-accent);
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}

.link-btn.danger {
  color: #ef4444;
}

.form-divider {
  margin: 4px 0 14px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-text);
}

.settings-overlay-enter-active,
.settings-overlay-leave-active {
  transition: opacity 0.28s ease;
}

.settings-overlay-enter-from,
.settings-overlay-leave-to {
  opacity: 0;
}

.settings-panel-enter-active,
.settings-panel-leave-active {
  transition: transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.settings-panel-enter-from,
.settings-panel-leave-to {
  transform: translateX(100%);
}
</style>
