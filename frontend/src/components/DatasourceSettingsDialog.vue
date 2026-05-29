<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createDataSource,
  testDataSourceConnection,
  updateDataSource,
} from '../api/datasource'
import type { DataSource, DataSourceForm } from '../types/datasource'

const props = defineProps<{
  visible: boolean
  editing?: DataSource | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const testing = ref(false)
const saving = ref(false)

const form = reactive<DataSourceForm>({
  name: '',
  dbType: 'mysql',
  host: '127.0.0.1',
  port: 3306,
  databaseName: '',
  username: 'root',
  password: '',
})

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})

const isEditing = computed(() => !!props.editing)

watch(
  () => props.visible,
  (visible) => {
    if (!visible) return
    if (props.editing) {
      form.name = props.editing.name
      form.dbType = props.editing.dbType
      form.host = props.editing.host
      form.port = props.editing.port
      form.databaseName = props.editing.databaseName
      form.username = props.editing.username
      form.password = ''
    } else {
      resetForm()
    }
  },
)

watch(
  () => form.dbType,
  (type) => {
    if (type === 'mysql' && form.port === 5432) form.port = 3306
    if ((type === 'pg' || type === 'postgresql') && form.port === 3306) form.port = 5432
  },
)

function resetForm() {
  form.name = ''
  form.dbType = 'mysql'
  form.host = '127.0.0.1'
  form.port = 3306
  form.databaseName = ''
  form.username = 'root'
  form.password = ''
}

async function handleTest() {
  if (!form.password) {
    ElMessage.warning('请先填写密码')
    return
  }
  testing.value = true
  try {
    await testDataSourceConnection({ ...form })
    ElMessage.success('连接成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '连接失败')
  } finally {
    testing.value = false
  }
}

async function handleSave() {
  if (!form.name || !form.host || !form.databaseName || !form.username) {
    ElMessage.warning('请填写完整配置')
    return
  }
  if (!form.password && !isEditing.value) {
    ElMessage.warning('请填写密码')
    return
  }
  if (isEditing.value && !form.password) {
    ElMessage.warning('更新配置需重新填写密码')
    return
  }

  saving.value = true
  try {
    if (isEditing.value && props.editing) {
      await updateDataSource(props.editing.id, { ...form })
      ElMessage.success('数据源已更新')
    } else {
      await createDataSource({ ...form })
      ElMessage.success('数据源已添加')
    }
    emit('saved')
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    class="ds-theme-dialog"
    :title="isEditing ? '编辑数据库连接' : '添加数据库连接'"
    width="520px"
    destroy-on-close
    align-center
  >
    <el-form label-position="top" class="ds-form" @submit.prevent>
      <el-form-item label="名称">
        <el-input v-model="form.name" placeholder="例如：本地 ChatBI" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.dbType" style="width: 100%">
          <el-option label="MySQL" value="mysql" />
          <el-option label="PostgreSQL" value="postgresql" />
        </el-select>
      </el-form-item>
      <div class="form-row">
        <el-form-item label="主机" class="flex-2">
          <el-input v-model="form.host" placeholder="127.0.0.1" />
        </el-form-item>
        <el-form-item label="端口" class="flex-1">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
      </div>
      <el-form-item label="数据库">
        <el-input v-model="form.databaseName" placeholder="chatbi" />
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <button class="ds-btn ghost" @click="dialogVisible = false">取消</button>
        <button class="ds-btn ghost" :disabled="testing" @click="handleTest">
          {{ testing ? '测试中...' : '测试连接' }}
        </button>
        <button class="ds-btn primary" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.ds-form :deep(.el-form-item__label) {
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
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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
.ds-btn.ghost:hover {
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
</style>
