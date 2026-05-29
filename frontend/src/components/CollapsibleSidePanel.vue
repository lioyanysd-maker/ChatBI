<script setup lang="ts">
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const props = defineProps<{
  side: 'left' | 'right'
  title: string
  collapsed: boolean
}>()

const emit = defineEmits<{
  'update:collapsed': [value: boolean]
}>()

function toggle() {
  emit('update:collapsed', !props.collapsed)
}
</script>

<template>
  <aside class="side-panel" :class="[side, { collapsed }]">
    <div v-if="!collapsed" class="panel-inner">
      <div class="panel-header">
        <span class="panel-title">{{ title }}</span>
        <button class="collapse-btn" :title="`收起${title}`" @click="toggle">
          <el-icon :size="14">
            <ArrowLeft v-if="side === 'left'" />
            <ArrowRight v-else />
          </el-icon>
        </button>
      </div>
      <div class="panel-body">
        <slot />
      </div>
    </div>

    <button
      v-else
      class="expand-rail"
      :title="`展开${title}`"
      @click="toggle"
    >
      <el-icon :size="16">
        <ArrowRight v-if="side === 'left'" />
        <ArrowLeft v-else />
      </el-icon>
      <span class="rail-label">{{ title }}</span>
    </button>
  </aside>
</template>

<style scoped>
.side-panel {
  flex-shrink: 0;
  height: 100%;
  background: var(--ds-sidebar);
  transition: width 0.28s cubic-bezier(0.22, 1, 0.36, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.side-panel.left {
  width: 260px;
  border-right: 1px solid var(--ds-border);
}

.side-panel.right {
  width: 260px;
  border-left: 1px solid var(--ds-border);
}

.side-panel.collapsed {
  width: 44px;
}

.panel-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 260px;
  animation: panelSlideIn 0.32s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.side-panel.left .panel-inner {
  animation-name: panelSlideInLeft;
}

.side-panel.right .panel-inner {
  animation-name: panelSlideInRight;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px 10px;
  flex-shrink: 0;
}

.panel-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--ds-text-secondary);
  letter-spacing: 0.04em;
}

.collapse-btn {
  width: 28px;
  height: 28px;
  border: 1px solid var(--ds-border);
  border-radius: 8px;
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.collapse-btn:hover {
  border-color: var(--ds-accent);
  color: var(--ds-accent);
}

.panel-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 10px 12px;
}

.expand-rail {
  width: 44px;
  height: 100%;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-top: 16px;
  color: var(--ds-text-secondary);
  transition: background 0.15s, color 0.15s;
}

.expand-rail:hover {
  background: rgba(77, 107, 254, 0.06);
  color: var(--ds-accent);
}

.rail-label {
  writing-mode: vertical-rl;
  font-size: 12px;
  letter-spacing: 2px;
}

@keyframes panelSlideInLeft {
  from {
    opacity: 0;
    transform: translateX(-24px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes panelSlideInRight {
  from {
    opacity: 0;
    transform: translateX(24px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
