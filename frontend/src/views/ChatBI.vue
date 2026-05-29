<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, Setting } from '@element-plus/icons-vue'
import { deleteDataSource, fetchDataSources } from '../api/datasource'
import { executeSql, fetchSchema, fetchSessionMessages, fetchSessions, deleteSessions, queryChatBIStream } from '../api/chatbi'
import ChartRenderer from '../components/ChartRenderer.vue'
import MarkdownContent from '../components/MarkdownContent.vue'
import ConversationSidebar from '../components/ConversationSidebar.vue'
import CollapsibleSidePanel from '../components/CollapsibleSidePanel.vue'
import DatasourceSidebar from '../components/DatasourceSidebar.vue'
import SettingsDrawer from '../components/SettingsDrawer.vue'
import TableWhitelistDialog from '../components/TableWhitelistDialog.vue'
import ColumnSemanticDialog from '../components/ColumnSemanticDialog.vue'
import QueryTemplateDialog from '../components/QueryTemplateDialog.vue'
import { fetchQueryTemplates } from '../api/template'
import appIcon from '../assets/app-icon.png'
import { copyText } from '../utils/export'
import type { QueryResponse, SessionSummary } from '../types/chatbi'
import type { DataSource } from '../types/datasource'
import type { QueryTemplate } from '../types/template'
import type { SettingsTab } from '../types/settings'

interface MessageItem {
  role: 'user' | 'assistant'
  content: string
  sql?: string
  result?: QueryResponse
  loading?: boolean
  answerType?: 'query' | 'chat'
  rowCount?: number
  executionTimeMs?: number
}

const question = ref('')
const chartType = ref('auto')
const sessionId = ref(localStorage.getItem('chatbi_session_id') || '')
const selectedDataSourceId = ref<number | null>(
  localStorage.getItem('chatbi_datasource_id')
    ? Number(localStorage.getItem('chatbi_datasource_id'))
    : null,
)
const messages = ref<MessageItem[]>([])
const loading = ref(false)
const schemaTables = ref<string[]>([])
const chatBodyRef = ref<HTMLDivElement>()
const inputRef = ref<HTMLTextAreaElement>()
const convSidebarRef = ref<InstanceType<typeof ConversationSidebar>>()

const datasources = ref<DataSource[]>([])
const dsLoading = ref(false)
const sessions = ref<SessionSummary[]>([])
const sessionsLoading = ref(false)
const settingsVisible = ref(false)
const settingsTab = ref<SettingsTab>('llm')
const editingDataSource = ref<DataSource | null>(null)
const convPanelCollapsed = ref(localStorage.getItem('chatbi_conv_collapsed') === '1')
const dsPanelCollapsed = ref(localStorage.getItem('chatbi_ds_collapsed') === '1')
const draftSessionActive = ref(false)
const loadingStep = ref('')
const editingSqlIndex = ref<number | null>(null)
const sqlDrafts = ref<Record<number, string>>({})
const whitelistVisible = ref(false)
const whitelistTarget = ref<DataSource | null>(null)
const semanticVisible = ref(false)
const semanticTarget = ref<DataSource | null>(null)
const semanticTables = ref<string[]>([])
const templateDialogVisible = ref(false)
const queryTemplates = ref<QueryTemplate[]>([])

let progressTimer: ReturnType<typeof setInterval> | null = null
const progressSteps = [
  '正在理解您的问题…',
  '正在生成 SQL…',
  '正在查询数据库…',
  '正在生成图表与解释…',
]

const queryExamples = computed(() =>
  queryTemplates.value.filter((t) => t.category !== 'chat'),
)

const chatExamples = computed(() =>
  queryTemplates.value.filter((t) => t.category === 'chat'),
)

const fallbackQueryExamples = [
  '总共有多少个商品？',
  '每个分类的商品数量是多少？',
  '商品与分类都有哪些？',
]

const fallbackChatExamples = [
  '这个数据库主要存储了什么信息？',
  '有哪些表，分别是什么用途？',
  '帮我介绍一下数据结构',
]

const displayQueryExamples = computed(() => {
  if (queryExamples.value.length) return queryExamples.value
  return fallbackQueryExamples.map((q, i) => ({
    id: -i - 1,
    title: q,
    question: q,
    category: 'query' as const,
    chartType: 'auto',
    sortOrder: i,
  }))
})

const displayChatExamples = computed(() => {
  if (chatExamples.value.length) return chatExamples.value
  return fallbackChatExamples.map((q, i) => ({
    id: -i - 100,
    title: q,
    question: q,
    category: 'chat' as const,
    chartType: 'auto',
    sortOrder: i,
  }))
})

const hasMessages = computed(() => messages.value.length > 0)

async function loadTemplates() {
  try {
    queryTemplates.value = await fetchQueryTemplates(selectedDataSourceId.value)
  } catch {
    queryTemplates.value = []
  }
}

async function loadDataSources() {
  dsLoading.value = true
  try {
    datasources.value = await fetchDataSources()
  } catch {
    datasources.value = []
  } finally {
    dsLoading.value = false
  }
}

async function loadSessions() {
  sessionsLoading.value = true
  try {
    sessions.value = await fetchSessions()
  } catch {
    sessions.value = []
  } finally {
    sessionsLoading.value = false
  }
}

async function loadSchema() {
  try {
    const schema = await fetchSchema(selectedDataSourceId.value)
    schemaTables.value = schema.tables || []
  } catch {
    schemaTables.value = []
  }
}

function autoResizeInput() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 160)}px`
}

function startProgress() {
  let step = 0
  loadingStep.value = progressSteps[0]
  progressTimer = setInterval(() => {
    step = Math.min(step + 1, progressSteps.length - 1)
    loadingStep.value = progressSteps[step]
  }, 2800)
}

function stopProgress() {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  loadingStep.value = ''
}

function sanitizeStreamText(text: string) {
  return text.replace(/\*\*([^*]+)\*\*/g, '$1').replace(/\*\*/g, '')
}

async function sendQuestion(text?: string, options?: { chartType?: string }) {
  const q = (text ?? question.value).trim()
  if (!q || loading.value) return

  question.value = ''
  nextTick(autoResizeInput)

  messages.value.push({ role: 'user', content: q })
  const assistantMsg: MessageItem = { role: 'assistant', content: '', loading: true }
  messages.value.push(assistantMsg)
  loading.value = true
  loadingStep.value = progressSteps[0]
  scrollToBottom()

  let streamed = false
  const effectiveChartType = options?.chartType ?? chartType.value

  try {
    await queryChatBIStream(
      {
        question: q,
        sessionId: sessionId.value || undefined,
        chartType: effectiveChartType,
        dataSourceId: selectedDataSourceId.value,
      },
      {
        onStep: (step) => {
          loadingStep.value = step
          scrollToBottom()
        },
        onSql: (sql) => {
          assistantMsg.sql = sql
          assistantMsg.answerType = 'query'
        },
        onChart: (chartData) => {
          assistantMsg.answerType = 'query'
          assistantMsg.result = {
            question: q,
            naturalExplanation: assistantMsg.content,
            generatedSql: assistantMsg.sql,
            chartData,
            rowCount: chartData.rows?.length ?? 0,
            executionTimeMs: 0,
          }
          scrollToBottom()
        },
        onDelta: (text) => {
          if (!streamed) {
            streamed = true
            assistantMsg.loading = false
          }
          assistantMsg.content += sanitizeStreamText(text)
          scrollToBottom()
        },
        onDone: (result) => {
          if (result.sessionId) {
            sessionId.value = result.sessionId
            localStorage.setItem('chatbi_session_id', result.sessionId)
            draftSessionActive.value = false
          }
          assistantMsg.loading = false
          assistantMsg.content = result.naturalExplanation || assistantMsg.content || '完成'
          assistantMsg.answerType = result.answerType ?? 'query'
          assistantMsg.sql = result.generatedSql
          assistantMsg.result = result
        },
        onError: (message) => {
          throw new Error(message)
        },
      },
    )
    await loadSessions()
  } catch (error) {
    assistantMsg.loading = false
    assistantMsg.content = error instanceof Error ? error.message : '请求失败'
    ElMessage.error(assistantMsg.content)
  } finally {
    loadingStep.value = ''
    loading.value = false
    scrollToBottom()
  }
}

async function copySql(sql: string) {
  try {
    await copyText(sql)
    ElMessage.success('SQL 已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

function startEditSql(index: number, sql: string) {
  editingSqlIndex.value = index
  sqlDrafts.value[index] = sql
}

function cancelEditSql() {
  editingSqlIndex.value = null
}

async function runEditedSql(index: number) {
  const sql = sqlDrafts.value[index]?.trim()
  if (!sql || loading.value) return

  const msg = messages.value[index]
  if (!msg || msg.role !== 'assistant') return

  msg.loading = true
  editingSqlIndex.value = null
  loading.value = true
  startProgress()
  scrollToBottom()

  try {
    const result = await executeSql({
      sql,
      sessionId: sessionId.value || undefined,
      chartType: chartType.value,
      dataSourceId: selectedDataSourceId.value,
      question: '手动执行 SQL',
    })
    if (result.sessionId) {
      sessionId.value = result.sessionId
      localStorage.setItem('chatbi_session_id', result.sessionId)
    }
    msg.loading = false
    msg.content = result.naturalExplanation || '完成'
    msg.answerType = 'query'
    msg.sql = result.generatedSql
    msg.result = result
    msg.rowCount = result.rowCount
    msg.executionTimeMs = result.executionTimeMs
    await loadSessions()
    ElMessage.success('SQL 执行成功')
  } catch (error) {
    msg.loading = false
    msg.content = error instanceof Error ? error.message : 'SQL 执行失败'
    ElMessage.error(msg.content)
  } finally {
    stopProgress()
    loading.value = false
    scrollToBottom()
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendQuestion()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

function newSession() {
  sessionId.value = ''
  localStorage.removeItem('chatbi_session_id')
  messages.value = []
  draftSessionActive.value = true
}

async function loadSession(id: string) {
  if (loading.value) return
  draftSessionActive.value = false
  sessionId.value = id
  localStorage.setItem('chatbi_session_id', id)
  editingSqlIndex.value = null
  try {
    const sessionData = await fetchSessionMessages(id)
    if (sessionData.dataSourceId != null) {
      onSelectDataSource(sessionData.dataSourceId)
    }
    messages.value = sessionData.messages.map((m) => ({
      role: m.role,
      content: m.content,
      sql: m.sql,
      answerType: m.answerType,
      rowCount: m.rowCount,
      executionTimeMs: m.executionTimeMs,
      loading: false,
      result:
        m.chartData && m.answerType !== 'chat'
          ? {
              question: '',
              naturalExplanation: m.content,
              generatedSql: m.sql,
              chartData: m.chartData,
              rowCount: m.rowCount ?? 0,
              executionTimeMs: m.executionTimeMs ?? 0,
            }
          : undefined,
    }))
    scrollToBottom()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载对话失败')
  }
}

function openSettings(tab: SettingsTab = 'llm') {
  settingsTab.value = tab
  if (tab === 'llm') {
    editingDataSource.value = null
  }
  settingsVisible.value = true
}

function openWhitelist(item: DataSource) {
  whitelistTarget.value = item
  whitelistVisible.value = true
}

function openSemantics(item: DataSource) {
  semanticTarget.value = item
  semanticVisible.value = true
  fetchSchema(item.id)
    .then((schema) => {
      semanticTables.value = schema.tables || []
    })
    .catch(() => {
      semanticTables.value = []
    })
}

function onEditDataSource(item: DataSource) {
  editingDataSource.value = item
  openSettings('database')
}

function onSelectDataSource(id: number | null) {
  selectedDataSourceId.value = id
  if (id == null) {
    localStorage.removeItem('chatbi_datasource_id')
  } else {
    localStorage.setItem('chatbi_datasource_id', String(id))
  }
  loadSchema()
}

async function onRemoveDataSource(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该数据源吗？', '提示', {
      type: 'warning',
      customClass: 'ds-theme-message-box',
    })
    await deleteDataSource(id)
    ElMessage.success('已删除')
    if (selectedDataSourceId.value === id) {
      onSelectDataSource(null)
    }
    await loadDataSources()
  } catch {
    // cancelled
  }
}

async function onDataSourceSaved() {
  editingDataSource.value = null
  await loadDataSources()
  await loadSchema()
}

async function onDeleteSessions(ids: string[]) {
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个对话吗？`, '提示', {
      type: 'warning',
      customClass: 'ds-theme-message-box',
    })
    await deleteSessions(ids)
    ElMessage.success('已删除')
    convSidebarRef.value?.exitManageMode()
    if (sessionId.value && ids.includes(sessionId.value)) {
      newSession()
    }
    await loadSessions()
  } catch {
    // cancelled
  }
}

watch(selectedDataSourceId, () => {
  loadSchema()
  loadTemplates()
})

watch(convPanelCollapsed, (v) => localStorage.setItem('chatbi_conv_collapsed', v ? '1' : '0'))
watch(dsPanelCollapsed, (v) => localStorage.setItem('chatbi_ds_collapsed', v ? '1' : '0'))

onMounted(async () => {
  await Promise.all([loadDataSources(), loadSessions(), loadTemplates()])
  await loadSchema()
  if (sessionId.value) {
    await loadSession(sessionId.value)
  }
})
</script>

<template>
  <div class="ds-page">
    <header class="ds-header">
      <div class="brand">
        <img :src="appIcon" alt="ChatBI" class="brand-icon" />
        <span class="brand-name">ChatBI</span>
      </div>
      <div class="header-actions">
        <button class="icon-btn" title="设置" @click="openSettings()">
          <el-icon :size="18"><Setting /></el-icon>
        </button>
      </div>
    </header>

    <div class="ds-body">
      <CollapsibleSidePanel
        v-model:collapsed="convPanelCollapsed"
        side="left"
        title="历史对话"
      >
        <ConversationSidebar
          ref="convSidebarRef"
          :sessions="sessions"
          :loading="sessionsLoading"
          :active-id="sessionId"
          :draft-active="draftSessionActive"
          @select="loadSession"
          @new-chat="newSession"
          @delete="onDeleteSessions"
        />
      </CollapsibleSidePanel>

      <main class="ds-main">
        <section ref="chatBodyRef" class="chat-scroll">
          <div class="chat-inner">
            <div v-if="!hasMessages" class="welcome">
              <img :src="appIcon" alt="ChatBI" class="welcome-logo" />
              <h2 class="welcome-title">有什么可以帮到你？</h2>
              <p class="welcome-sub">连接数据库后，可查询数据或了解库表结构</p>

              <div class="suggest-group">
                <div class="suggest-label-row">
                  <span class="suggest-label">数据查询</span>
                  <button type="button" class="suggest-manage" @click="templateDialogVisible = true">管理</button>
                </div>
                <div class="suggest-list">
                  <button
                    v-for="item in displayQueryExamples"
                    :key="item.id"
                    class="suggest-chip"
                    @click="sendQuestion(item.question, { chartType: item.chartType })"
                  >
                    {{ item.title }}
                  </button>
                </div>
              </div>

              <div class="suggest-group">
                <div class="suggest-label-row">
                  <span class="suggest-label">描述问答</span>
                </div>
                <div class="suggest-list">
                  <button
                    v-for="item in displayChatExamples"
                    :key="item.id"
                    class="suggest-chip"
                    @click="sendQuestion(item.question)"
                  >
                    {{ item.title }}
                  </button>
                </div>
              </div>
            </div>

            <div v-for="(msg, index) in messages" :key="index" class="msg-row" :class="msg.role">
              <template v-if="msg.role === 'user'">
                <div class="user-bubble">{{ msg.content }}</div>
              </template>

              <template v-else>
                <div class="avatar assistant-avatar">AI</div>
                <div class="msg-content">
                  <div v-if="msg.loading" class="typing">
                    <p v-if="loadingStep" class="loading-step">{{ loadingStep }}</p>
                    <div class="typing-dots">
                      <span class="dot" /><span class="dot" /><span class="dot" />
                    </div>
                  </div>
                  <template v-else>
                    <MarkdownContent :content="msg.content" class="assistant-text" />

                    <details v-if="msg.sql && msg.answerType !== 'chat'" class="sql-details">
                      <summary>查看 SQL</summary>
                      <div class="sql-actions">
                        <button type="button" class="sql-action-btn" @click="copySql(msg.sql!)">复制</button>
                        <button
                          v-if="editingSqlIndex !== index"
                          type="button"
                          class="sql-action-btn"
                          @click="startEditSql(index, msg.sql!)"
                        >
                          编辑
                        </button>
                      </div>
                      <pre v-if="editingSqlIndex !== index">{{ msg.sql }}</pre>
                      <div v-else class="sql-edit-wrap">
                        <textarea v-model="sqlDrafts[index]" class="sql-editor" rows="6" />
                        <div class="sql-edit-actions">
                          <button type="button" class="sql-action-btn primary" @click="runEditedSql(index)">
                            执行 SQL
                          </button>
                          <button type="button" class="sql-action-btn" @click="cancelEditSql">取消</button>
                        </div>
                      </div>
                    </details>

                    <ChartRenderer
                      v-if="msg.result?.chartData && msg.answerType !== 'chat'"
                      :key="`${index}-${msg.result.generatedSql}`"
                      :chart-data="msg.result.chartData"
                    />

                    <div v-if="msg.answerType === 'query' && (msg.result || msg.rowCount != null)" class="meta">
                      {{ msg.result?.rowCount ?? msg.rowCount }} 行
                      · {{ msg.result?.executionTimeMs ?? msg.executionTimeMs }} ms
                    </div>
                  </template>
                </div>
              </template>
            </div>
          </div>
        </section>

        <footer class="input-area">
          <div class="input-box">
            <textarea
              ref="inputRef"
              v-model="question"
              class="chat-input"
              rows="1"
              placeholder="给 ChatBI 发送消息，Shift+Enter 换行"
              @keydown="handleKeydown"
              @input="autoResizeInput"
            />
            <div class="input-toolbar">
              <el-select v-model="chartType" size="small" class="chart-select" popper-class="ds-theme-dropdown">
                <el-option label="自动图表" value="auto" />
                <el-option label="柱状图" value="bar" />
                <el-option label="折线图" value="line" />
                <el-option label="饼图" value="pie" />
                <el-option label="表格" value="table" />
              </el-select>
              <span v-if="schemaTables.length" class="table-hint">
                {{ schemaTables.length }} 张表可用
              </span>
              <button class="send-btn" :disabled="loading || !question.trim()" @click="sendQuestion()">
                <el-icon :size="18"><Promotion /></el-icon>
              </button>
            </div>
          </div>
          <p class="input-tip">ChatBI 可能会犯错，重要数据请核实 SQL 结果</p>
        </footer>
      </main>

      <CollapsibleSidePanel
        v-model:collapsed="dsPanelCollapsed"
        side="right"
        title="数据源"
      >
        <DatasourceSidebar
          :datasources="datasources"
          :loading="dsLoading"
          :selected-id="selectedDataSourceId"
          @select="onSelectDataSource"
          @edit="onEditDataSource"
          @remove="onRemoveDataSource"
          @whitelist="openWhitelist"
          @semantics="openSemantics"
        />
      </CollapsibleSidePanel>
    </div>

    <SettingsDrawer
      v-model:visible="settingsVisible"
      :datasources="datasources"
      :editing="editingDataSource"
      :initial-tab="settingsTab"
      @saved="onDataSourceSaved"
    />

    <TableWhitelistDialog
      v-model:visible="whitelistVisible"
      :data-source-id="whitelistTarget?.id ?? null"
      :data-source-name="whitelistTarget?.name"
    />

    <ColumnSemanticDialog
      v-model:visible="semanticVisible"
      :data-source-id="semanticTarget?.id ?? null"
      :data-source-name="semanticTarget?.name"
      :tables="semanticTables"
    />

    <QueryTemplateDialog
      v-model:visible="templateDialogVisible"
      :data-source-id="selectedDataSourceId"
      @changed="loadTemplates"
    />
  </div>
</template>

<style scoped>
.ds-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--ds-bg);
  overflow: hidden;
}

.ds-header {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-surface);
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}
.brand-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  object-fit: cover;
  display: block;
  flex-shrink: 0;
}
.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--ds-text);
}
.header-actions {
  display: flex;
  gap: 4px;
}
.icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--ds-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.icon-btn:hover {
  background: var(--ds-input-bg);
  color: var(--ds-text);
}

.ds-body {
  flex: 1;
  display: flex;
  min-height: 0;
}
.ds-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--ds-bg);
}

.chat-scroll {
  flex: 1;
  overflow-y: auto;
  scroll-behavior: smooth;
}
.chat-inner {
  max-width: 768px;
  margin: 0 auto;
  padding: 32px 24px 24px;
}

.welcome {
  text-align: center;
  padding: 48px 0 32px;
}
.welcome-logo {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  object-fit: cover;
  display: block;
  margin: 0 auto 20px;
}
.welcome-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 600;
  color: var(--ds-text);
}
.welcome-sub {
  margin: 0 0 36px;
  font-size: 14px;
  color: var(--ds-text-secondary);
}
.suggest-group {
  text-align: left;
  margin-bottom: 20px;
}
.suggest-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-left: 4px;
  padding-right: 4px;
}
.suggest-label {
  font-size: 12px;
  color: var(--ds-text-secondary);
}
.suggest-manage {
  border: none;
  background: none;
  padding: 0;
  font-size: 12px;
  color: var(--ds-accent);
  cursor: pointer;
}
.suggest-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.suggest-chip {
  padding: 10px 16px;
  border: 1px solid var(--ds-border);
  border-radius: 12px;
  background: var(--ds-surface);
  color: var(--ds-text);
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
  line-height: 1.4;
}
.suggest-chip:hover {
  border-color: var(--ds-accent);
  background: #f0f3ff;
}

.msg-row {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;
  align-items: flex-start;
}
.msg-row.user {
  justify-content: flex-end;
}
.msg-row.assistant .msg-content {
  flex: 1;
  min-width: 0;
  max-width: calc(100% - 44px);
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}
.assistant-avatar {
  background: var(--ds-accent);
  color: #fff;
}
.msg-content {
  min-width: 0;
}
.user-bubble {
  max-width: min(85%, 100%);
  padding: 12px 16px;
  background: var(--ds-user-bg);
  border-radius: 16px 16px 4px 16px;
  font-size: 15px;
  line-height: 1.6;
  color: var(--ds-text);
  word-break: break-word;
}
.assistant-text {
  min-width: 0;
}

.typing {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 0;
}
.loading-step {
  margin: 0;
  font-size: 13px;
  color: var(--ds-text-secondary);
}
.typing-dots {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ds-text-secondary);
  animation: bounce 1.2s infinite;
}
.dot:nth-child(2) { animation-delay: 0.15s; }
.dot:nth-child(3) { animation-delay: 0.3s; }
@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

.sql-details {
  margin-top: 12px;
  font-size: 13px;
}
.sql-details summary {
  cursor: pointer;
  color: var(--ds-text-secondary);
  user-select: none;
}
.sql-actions,
.sql-edit-actions {
  display: flex;
  gap: 8px;
  margin: 8px 0;
}
.sql-action-btn {
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  border-radius: 8px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}
.sql-action-btn.primary {
  border-color: var(--ds-accent);
  color: var(--ds-accent);
}
.sql-action-btn:hover {
  border-color: var(--ds-accent);
  color: var(--ds-accent);
}
.sql-details pre,
.sql-editor {
  margin: 0;
  padding: 12px;
  background: var(--ds-input-bg);
  border-radius: 8px;
  font-size: 12px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--ds-border);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
.sql-edit-wrap {
  margin-top: 4px;
}
.meta {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ds-text-secondary);
}

.input-area {
  flex-shrink: 0;
  padding: 12px 24px 16px;
  background: var(--ds-bg);
}
.input-box {
  max-width: 768px;
  margin: 0 auto;
  border: 1px solid var(--ds-border);
  border-radius: 16px;
  background: var(--ds-input-bg);
  padding: 12px 16px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.input-box:focus-within {
  border-color: var(--ds-accent);
  box-shadow: 0 0 0 3px rgba(77, 107, 254, 0.12);
  background: var(--ds-surface);
}
.chat-input {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  font-size: 15px;
  line-height: 1.6;
  color: var(--ds-text);
  font-family: inherit;
  min-height: 24px;
  max-height: 160px;
}
.chat-input::placeholder {
  color: var(--ds-text-secondary);
}
.input-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.chart-select {
  width: 110px;
}
.table-hint {
  flex: 1;
  font-size: 12px;
  color: var(--ds-text-secondary);
}
.send-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: var(--ds-accent);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, opacity 0.15s;
  margin-left: auto;
}
.send-btn:hover:not(:disabled) {
  background: var(--ds-accent-hover);
}
.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.input-tip {
  max-width: 768px;
  margin: 8px auto 0;
  text-align: center;
  font-size: 12px;
  color: var(--ds-text-secondary);
}
</style>
