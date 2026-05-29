<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createQueryTemplate, deleteQueryTemplate, fetchQueryTemplates } from '../api/template'
import type { QueryTemplate, QueryTemplateForm } from '../types/template'

const props = defineProps<{
  visible: boolean
  dataSourceId?: number | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  changed: []
}>()

const loading = ref(false)
const saving = ref(false)
const items = ref<QueryTemplate[]>([])
const form = ref<QueryTemplateForm>({
  title: '',
  question: '',
  category: 'query',
  chartType: 'auto',
  sortOrder: 0,
  dataSourceId: null,
})

const sortedItems = computed(() =>
  [...items.value].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)),
)

async function load() {
  loading.value = true
  try {
    items.value = await fetchQueryTemplates(props.dataSourceId ?? null)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载常用问题失败')
    items.value = []
  } finally {
    loading.value = false
  }
}

async function addTemplate() {
  if (!form.value.title.trim() || !form.value.question.trim()) {
    ElMessage.warning('请填写标题和问题')
    return
  }
  saving.value = true
  try {
    await createQueryTemplate({
      ...form.value,
      dataSourceId: props.dataSourceId ?? null,
      title: form.value.title.trim(),
      question: form.value.question.trim(),
    })
    form.value = {
      title: '',
      question: '',
      category: 'query',
      chartType: 'auto',
      sortOrder: 0,
      dataSourceId: props.dataSourceId ?? null,
    }
    await load()
    emit('changed')
    ElMessage.success('已添加')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '添加失败')
  } finally {
    saving.value = false
  }
}

async function removeTemplate(item: QueryTemplate) {
  try {
    await ElMessageBox.confirm(`删除「${item.title}」？`, '确认', { type: 'warning' })
    await deleteQueryTemplate(item.id)
    await load()
    emit('changed')
    ElMessage.success('已删除')
  } catch {
    // cancelled or failed
  }
}

function close() {
  emit('update:visible', false)
}

watch(
  () => [props.visible, props.dataSourceId] as const,
  ([visible]) => {
    if (visible) {
      form.value.dataSourceId = props.dataSourceId ?? null
      load()
    }
  },
  { immediate: true },
)
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="管理常用问题"
    width="640px"
    class="ds-theme-dialog"
    @close="close"
  >
    <p class="tip">欢迎页展示的快捷问题。留空数据源表示通用模板，切换数据源时会一并加载。</p>

    <div class="add-form">
      <el-input v-model="form.title" size="small" placeholder="展示标题，如：商品总数" />
      <el-input v-model="form.question" size="small" placeholder="实际发送的问题" />
      <el-select v-model="form.category" size="small" class="cat-select">
        <el-option label="数据查询" value="query" />
        <el-option label="描述问答" value="chat" />
      </el-select>
      <el-select v-model="form.chartType" size="small" class="chart-select">
        <el-option label="自动图表" value="auto" />
        <el-option label="柱状图" value="bar" />
        <el-option label="折线图" value="line" />
        <el-option label="饼图" value="pie" />
        <el-option label="表格" value="table" />
      </el-select>
      <el-button type="primary" size="small" :loading="saving" @click="addTemplate">添加</el-button>
    </div>

    <el-table v-loading="loading" :data="sortedItems" max-height="360" size="small" empty-text="暂无模板">
      <el-table-column prop="title" label="标题" min-width="100" />
      <el-table-column prop="question" label="问题" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          {{ row.category === 'chat' ? '描述' : '查询' }}
        </template>
      </el-table-column>
      <el-table-column prop="chartType" label="图表" width="70" />
      <el-table-column label="操作" width="70" align="center">
        <template #default="{ row }">
          <button type="button" class="link danger" @click="removeTemplate(row)">删除</button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="close">关闭</el-button>
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

.add-form {
  display: grid;
  grid-template-columns: 1fr 1.4fr 100px 100px auto;
  gap: 8px;
  margin-bottom: 12px;
}

.link {
  border: none;
  background: none;
  padding: 0;
  font-size: 12px;
  color: var(--ds-accent);
  cursor: pointer;
}

.link.danger {
  color: #e53935;
}

@media (max-width: 640px) {
  .add-form {
    grid-template-columns: 1fr;
  }
}
</style>
