<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { SessionSummary } from '../types/chatbi'

const props = defineProps<{
  sessions: SessionSummary[]
  loading: boolean
  activeId: string
  draftActive?: boolean
}>()

const emit = defineEmits<{
  select: [sessionId: string]
  newChat: []
  delete: [sessionIds: string[]]
}>()

const manageMode = ref(false)
const selectedIds = ref<string[]>([])

const allSelected = computed(
  () => props.sessions.length > 0 && selectedIds.value.length === props.sessions.length,
)

watch(
  () => props.sessions,
  (sessions) => {
    const idSet = new Set(sessions.map((item) => item.sessionId))
    selectedIds.value = selectedIds.value.filter((id) => idSet.has(id))
    if (manageMode.value && sessions.length === 0) {
      manageMode.value = false
    }
  },
)

function formatTime(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function enterManageMode() {
  if (!props.sessions.length) return
  manageMode.value = true
  selectedIds.value = []
}

function exitManageMode() {
  manageMode.value = false
  selectedIds.value = []
}

function toggleSelectAll() {
  if (allSelected.value) {
    selectedIds.value = []
    return
  }
  selectedIds.value = props.sessions.map((item) => item.sessionId)
}

function isSelected(sessionId: string) {
  return selectedIds.value.includes(sessionId)
}

function toggleSelected(sessionId: string) {
  if (isSelected(sessionId)) {
    selectedIds.value = selectedIds.value.filter((id) => id !== sessionId)
  } else {
    selectedIds.value = [...selectedIds.value, sessionId]
  }
}

function handleItemClick(sessionId: string) {
  if (manageMode.value) {
    toggleSelected(sessionId)
    return
  }
  emit('select', sessionId)
}

function handleDelete() {
  if (!selectedIds.value.length) return
  emit('delete', [...selectedIds.value])
}

defineExpose({
  exitManageMode,
})
</script>

<template>
  <div class="conv-panel">
    <div class="panel-actions">
      <button class="new-btn" title="新对话" @click="emit('newChat')">+ 新对话</button>
    </div>

    <div v-if="loading" class="conv-loading">
      <div v-for="i in 5" :key="i" class="conv-skeleton" :style="{ animationDelay: `${i * 0.07}s` }" />
    </div>

    <div v-else class="conv-list">
      <div
        v-if="draftActive"
        class="conv-row"
      >
        <button class="conv-item draft-item active" @click="emit('newChat')">
          <div class="conv-title">新对话</div>
          <div class="conv-meta">
            <span>当前对话</span>
          </div>
        </button>
      </div>

      <div
        v-for="(item, index) in sessions"
        :key="item.sessionId"
        class="conv-row"
        :class="{ 'manage-mode': manageMode }"
      >
        <button
          class="conv-item"
          :class="{ active: !manageMode && !draftActive && activeId === item.sessionId, selected: manageMode && isSelected(item.sessionId) }"
          :style="{ animationDelay: `${(draftActive ? index + 1 : index) * 0.06}s` }"
          @click="handleItemClick(item.sessionId)"
        >
          <div class="conv-title">{{ item.title || '新对话' }}</div>
          <div class="conv-meta">
            <span>{{ item.messageCount }} 条</span>
            <span>{{ formatTime(item.updatedAt) }}</span>
          </div>
        </button>

        <Transition name="check-pop">
          <label
            v-if="manageMode"
            class="conv-check"
            @click.stop
          >
            <input
              type="checkbox"
              :checked="isSelected(item.sessionId)"
              @change="toggleSelected(item.sessionId)"
            >
          </label>
        </Transition>
      </div>

      <div v-if="!draftActive && !sessions.length" class="conv-empty">暂无历史对话</div>
    </div>

    <div class="panel-footer">
      <template v-if="!manageMode">
        <button
          class="manage-btn"
          :disabled="!sessions.length"
          @click="enterManageMode"
        >
          管理对话
        </button>
      </template>
      <template v-else>
        <button class="footer-btn" @click="toggleSelectAll">
          {{ allSelected ? '取消全选' : '全选' }}
        </button>
        <button
          class="footer-btn danger"
          :disabled="!selectedIds.length"
          @click="handleDelete"
        >
          删除{{ selectedIds.length ? ` (${selectedIds.length})` : '' }}
        </button>
        <button class="footer-btn" @click="exitManageMode">取消</button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.conv-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-actions {
  padding: 0 2px 10px;
  flex-shrink: 0;
}

.new-btn {
  width: 100%;
  height: 34px;
  border: 1px dashed var(--ds-border);
  border-radius: 10px;
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.new-btn:hover {
  border-color: var(--ds-accent);
  color: var(--ds-accent);
  background: #f0f3ff;
}

.conv-list,
.conv-loading {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.conv-row {
  display: flex;
  align-items: stretch;
  gap: 8px;
}

.conv-row.manage-mode .conv-item {
  flex: 1;
  min-width: 0;
}

.conv-item {
  width: 100%;
  text-align: left;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, transform 0.15s;
  animation: slideInFromLeft 0.4s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.conv-item:hover {
  background: rgba(77, 107, 254, 0.06);
  transform: translateX(2px);
}

.conv-row.manage-mode .conv-item:hover {
  transform: none;
}

.conv-item.active,
.conv-item.selected {
  background: rgba(77, 107, 254, 0.1);
  border-color: rgba(77, 107, 254, 0.25);
}

.draft-item {
  animation: popInDraft 0.34s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.conv-check {
  flex-shrink: 0;
  width: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.conv-check input {
  width: 16px;
  height: 16px;
  accent-color: var(--ds-accent);
  cursor: pointer;
}

.panel-footer {
  flex-shrink: 0;
  padding-top: 10px;
  display: flex;
  gap: 6px;
}

.manage-btn,
.footer-btn {
  flex: 1;
  height: 34px;
  border: 1px solid var(--ds-border);
  border-radius: 10px;
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.manage-btn:hover:not(:disabled),
.footer-btn:hover:not(:disabled) {
  border-color: var(--ds-accent);
  color: var(--ds-accent);
}

.footer-btn.danger {
  color: #ef4444;
}

.footer-btn.danger:hover:not(:disabled) {
  border-color: #ef4444;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.06);
}

.manage-btn:disabled,
.footer-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.conv-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.conv-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--ds-text-secondary);
}

.conv-skeleton {
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(90deg, #eef0f4 25%, #e4e7ed 50%, #eef0f4 75%);
  background-size: 200% 100%;
  animation: slideInFromLeft 0.4s both, shimmer 1.2s infinite;
}

.conv-empty {
  font-size: 12px;
  color: var(--ds-text-secondary);
  text-align: center;
  padding: 24px 8px;
}

.check-pop-enter-active,
.check-pop-leave-active {
  transition: opacity 0.22s ease, transform 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

.check-pop-enter-from,
.check-pop-leave-to {
  opacity: 0;
  transform: translateX(12px);
}

@keyframes popInDraft {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes slideInFromLeft {
  from {
    opacity: 0;
    transform: translateX(-28px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
