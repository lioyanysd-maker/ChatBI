<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchTableWhitelist, updateTableWhitelist } from '../api/datasource'
import type { TableWhitelistItem } from '../types/datasource'

const props = defineProps<{
  visible: boolean
  dataSourceId: number | null
  dataSourceName?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const loading = ref(false)
const saving = ref(false)
const items = ref<TableWhitelistItem[]>([])

async function load() {
  if (props.dataSourceId == null) return
  loading.value = true
  try {
    items.value = await fetchTableWhitelist(props.dataSourceId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载表白名单失败')
    items.value = []
  } finally {
    loading.value = false
  }
}

async function save() {
  if (props.dataSourceId == null) return
  saving.value = true
  try {
    items.value = await updateTableWhitelist(props.dataSourceId, {
      items: items.value.map((item) => ({
        tableName: item.tableName,
        tableComment: item.tableComment,
        active: item.active,
      })),
    })
    ElMessage.success('表白名单已保存')
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
  () => [props.visible, props.dataSourceId] as const,
  ([visible, id]) => {
    if (visible && id != null) {
      load()
    }
  },
  { immediate: true },
)
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`表白名单 · ${dataSourceName || '数据源'}`"
    width="560px"
    class="ds-theme-dialog"
    @close="close"
  >
    <p class="tip">仅勾选的表可被 AI 查询。取消勾选可隐藏敏感或无关表。</p>
    <el-table v-loading="loading" :data="items" max-height="420" size="small" empty-text="暂无表">
      <el-table-column label="启用" width="70" align="center">
        <template #default="{ row }">
          <el-checkbox v-model="row.active" />
        </template>
      </el-table-column>
      <el-table-column prop="tableName" label="表名" min-width="160" />
      <el-table-column label="说明" min-width="180">
        <template #default="{ row }">
          <el-input v-model="row.tableComment" size="small" placeholder="可选中文说明" />
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
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
</style>
