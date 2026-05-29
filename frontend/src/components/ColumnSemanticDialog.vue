<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchColumnSemantics, updateColumnSemantics } from '../api/semantic'
import type { ColumnSemanticItem } from '../types/semantic'

const props = defineProps<{
  visible: boolean
  dataSourceId: number | null
  dataSourceName?: string
  tables?: string[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const loading = ref(false)
const saving = ref(false)
const selectedTable = ref('')
const items = ref<ColumnSemanticItem[]>([])

async function loadColumns() {
  if (props.dataSourceId == null || !selectedTable.value) return
  loading.value = true
  try {
    items.value = await fetchColumnSemantics(props.dataSourceId, selectedTable.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载字段失败')
    items.value = []
  } finally {
    loading.value = false
  }
}

async function save() {
  if (props.dataSourceId == null || !selectedTable.value) return
  saving.value = true
  try {
    await updateColumnSemantics(props.dataSourceId, {
      items: items.value.map((item) => ({
        tableName: selectedTable.value,
        columnName: item.columnName,
        businessName: item.businessName?.trim() || '',
        description: item.description?.trim() || '',
      })),
    })
    ElMessage.success('字段语义已保存')
    emit('update:visible', false)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function close() {
  emit('update:visible', false)
}

watch(
  () => [props.visible, props.dataSourceId, props.tables] as const,
  ([visible, id, tables]) => {
    if (visible && id != null) {
      selectedTable.value = tables?.[0] || ''
      if (selectedTable.value) {
        loadColumns()
      } else {
        items.value = []
      }
    }
  },
  { immediate: true },
)

watch(selectedTable, (table) => {
  if (props.visible && props.dataSourceId != null && table) {
    loadColumns()
  }
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`字段语义 · ${dataSourceName || '数据源'}`"
    width="680px"
    class="ds-theme-dialog"
    @close="close"
  >
    <p class="tip">为字段配置中文业务名与口径说明，AI 生成 SQL 与图表时会优先使用。</p>
    <div class="toolbar">
      <span class="label">选择表</span>
      <el-select
        v-model="selectedTable"
        size="small"
        class="table-select"
        placeholder="请选择表"
        :disabled="!tables?.length"
      >
        <el-option v-for="t in tables" :key="t" :label="t" :value="t" />
      </el-select>
    </div>
    <el-table v-loading="loading" :data="items" max-height="420" size="small" empty-text="请先选择表">
      <el-table-column prop="columnName" label="字段名" min-width="140" />
      <el-table-column label="业务名" min-width="160">
        <template #default="{ row }">
          <el-input v-model="row.businessName" size="small" placeholder="如：商品名称" />
        </template>
      </el-table-column>
      <el-table-column label="口径/说明" min-width="220">
        <template #default="{ row }">
          <el-input v-model="row.description" size="small" placeholder="可选，如：含税单价" />
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="!selectedTable" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--ds-text-secondary);
  line-height: 1.5;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.label {
  font-size: 13px;
  color: var(--ds-text-secondary);
}

.table-select {
  width: 220px;
}
</style>
