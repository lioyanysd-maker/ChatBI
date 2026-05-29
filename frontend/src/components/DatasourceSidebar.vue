<script setup lang="ts">
import { Coin, Connection, Monitor } from '@element-plus/icons-vue'
import type { DataSource } from '../types/datasource'

defineProps<{
  datasources: DataSource[]
  loading: boolean
  selectedId: number | null
}>()

const emit = defineEmits<{
  select: [id: number | null]
  edit: [item: DataSource]
  remove: [id: number]
  whitelist: [item: DataSource]
  semantics: [item: DataSource]
}>()

function dbTypeLabel(type: string) {
  if (type.startsWith('pg')) return 'PostgreSQL'
  if (type === 'oracle') return 'Oracle'
  return 'MySQL'
}

function dbIcon(type: string) {
  if (type.startsWith('pg')) return Connection
  return Coin
}
</script>

<template>
  <div class="ds-panel">
    <div v-if="loading" class="loading-wrap">
      <div v-for="i in 3" :key="i" class="card-skeleton" :style="{ animationDelay: `${i * 0.08}s` }" />
    </div>

    <div v-else-if="!datasources.length" class="empty-wrap">
      <p class="empty-text">暂无数据源</p>
      <p class="empty-tip">点击右上角设置添加</p>
    </div>

    <div v-else class="card-list">
      <div
        v-for="(item, index) in datasources"
        :key="item.id"
        class="ds-card"
        :class="{ active: selectedId === item.id }"
        :style="{ animationDelay: `${index * 0.08}s` }"
        @click="emit('select', item.id)"
      >
        <div class="card-icon">
          <el-icon :size="18"><component :is="dbIcon(item.dbType)" /></el-icon>
        </div>
        <div class="card-body">
          <div class="card-name">{{ item.name }}</div>
          <div class="card-meta">{{ dbTypeLabel(item.dbType) }} · {{ item.databaseName }}</div>
        </div>
        <div
          class="card-actions-shell"
          :class="{ 'is-open': selectedId === item.id }"
          @click.stop
        >
          <div class="card-actions-inner">
            <div class="card-actions">
              <button class="action-link" @click="emit('whitelist', item)">表白名单</button>
              <button class="action-link" @click="emit('semantics', item)">字段语义</button>
              <button class="action-link" @click="emit('edit', item)">编辑</button>
              <button class="action-link danger" @click="emit('remove', item.id)">删除</button>
            </div>
          </div>
        </div>
      </div>

      <div
        class="ds-card default-card"
        :class="{ active: selectedId === null }"
        :style="{ animationDelay: `${datasources.length * 0.08}s` }"
        @click="emit('select', null)"
      >
        <div class="card-icon default-icon">
          <el-icon :size="18"><Monitor /></el-icon>
        </div>
        <div class="card-body">
          <div class="card-name">默认数据库</div>
          <div class="card-meta">application.yml</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ds-panel {
  height: 100%;
}

.card-list,
.loading-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ds-card,
.card-skeleton {
  border-radius: 16px;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  padding: 12px;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
  animation: slideInFromRight 0.4s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.ds-card:hover {
  border-color: #c5cae9;
  box-shadow: var(--ds-shadow);
  transform: translateX(-2px);
}

.ds-card.active {
  border-color: var(--ds-accent);
  box-shadow: 0 0 0 1px var(--ds-accent);
}

.card-skeleton {
  height: 72px;
  cursor: default;
  background: linear-gradient(90deg, #eef0f4 25%, #e4e7ed 50%, #eef0f4 75%);
  background-size: 200% 100%;
  animation: slideInFromRight 0.4s both, shimmer 1.2s infinite;
}

.card-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: #eef1ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ds-accent);
  margin-bottom: 8px;
}

.default-icon {
  background: #e8f5e9;
  color: #43a047;
}

.card-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-text);
  margin-bottom: 2px;
}

.card-meta {
  font-size: 11px;
  color: var(--ds-text-secondary);
  line-height: 1.4;
}

.card-actions-shell {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.32s cubic-bezier(0.22, 1, 0.36, 1);
  pointer-events: none;
}

.card-actions-shell.is-open {
  grid-template-rows: 1fr;
  pointer-events: auto;
}

.card-actions-inner {
  overflow: hidden;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid transparent;
  opacity: 0;
  transform: translateY(-6px);
  transition:
    opacity 0.24s ease,
    transform 0.24s ease,
    border-color 0.24s ease;
}

.card-actions-shell.is-open .card-actions {
  opacity: 1;
  transform: translateY(0);
  border-top-color: var(--ds-border);
  transition-delay: 0.06s;
}

.card-actions-shell.is-open .action-link {
  animation: actionItemIn 0.28s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.card-actions-shell.is-open .action-link:nth-child(1) { animation-delay: 0.08s; }
.card-actions-shell.is-open .action-link:nth-child(2) { animation-delay: 0.12s; }
.card-actions-shell.is-open .action-link:nth-child(3) { animation-delay: 0.16s; }
.card-actions-shell.is-open .action-link:nth-child(4) { animation-delay: 0.2s; }

.action-link {
  border: none;
  background: none;
  padding: 0;
  font-size: 11px;
  color: var(--ds-accent);
  cursor: pointer;
}

.action-link.danger {
  color: #e53935;
}

.empty-wrap {
  text-align: center;
  padding: 32px 8px;
}

.empty-text {
  margin: 0;
  font-size: 13px;
  color: var(--ds-text-secondary);
}

.empty-tip {
  margin: 6px 0 0;
  font-size: 11px;
  color: var(--ds-text-secondary);
  opacity: 0.8;
}

@keyframes slideInFromRight {
  from {
    opacity: 0;
    transform: translateX(28px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes actionItemIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
