<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { ChartData } from '../types/chatbi'
import { formatCell, normalizeChartData, columnHeader } from '../utils/chartData'
import { exportChartDataCsv } from '../utils/export'

const props = defineProps<{
  chartData: ChartData
}>()

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const normalized = computed(() => normalizeChartData(props.chartData))

const tableColumns = computed(() => normalized.value.columns ?? [])
const tableRows = computed(() => normalized.value.rows ?? [])
const columnLabels = computed(() => normalized.value.columnLabels)

function toNumber(value: unknown): number {
  if (typeof value === 'number') return value
  if (value == null) return 0
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

async function renderChart() {
  await nextTick()
  const data = normalized.value

  if (data.type === 'table') {
    disposeChart()
    return
  }

  if (!chartRef.value) return

  await nextTick()
  requestAnimationFrame(() => {
    if (!chartRef.value) return

    if (!chart) {
      chart = echarts.init(chartRef.value)
    }

    const { type, xAxis, series } = data
    const categories = Array.isArray(xAxis) ? xAxis : []
    const values = Array.isArray(series) ? series : [series]

    if (type === 'kpi') {
      chart.setOption({
        title: {
          text: formatCell(series),
          left: 'center',
          top: 'center',
          textStyle: { fontSize: 36, fontWeight: 600 },
        },
        series: [],
      }, true)
      chart.resize()
      return
    }

    if (type === 'pie') {
      const pieData = categories.map((name, idx) => ({
        name: String(name),
        value: toNumber(values[idx]),
      }))
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        series: [{ type: 'pie', radius: '58%', center: ['50%', '45%'], data: pieData }],
      }, true)
      chart.resize()
      return
    }

    const chartSeriesType = type === 'line' ? 'line' : 'bar'
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 48, right: 24, top: 40, bottom: 48 },
      xAxis: { type: 'category', data: categories, axisLabel: { interval: 0, rotate: categories.length > 6 ? 30 : 0 } },
      yAxis: { type: 'value' },
      series: [{
        type: chartSeriesType,
        data: values.map(toNumber),
        itemStyle: { color: '#409eff' },
        smooth: chartSeriesType === 'line',
      }],
    }, true)
    chart.resize()
  })
}

function disposeChart() {
  chart?.dispose()
  chart = null
}

function handleResize() {
  chart?.resize()
}

function exportCsv() {
  exportChartDataCsv(normalized.value)
  ElMessage.success('CSV 已导出')
}

function downloadChart() {
  if (!chart) return
  const url = chart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
  const link = document.createElement('a')
  link.href = url
  link.download = 'chatbi-chart.png'
  link.click()
  ElMessage.success('图表已下载')
}

watch(normalized, renderChart, { deep: true })

onMounted(async () => {
  await renderChart()
  window.addEventListener('resize', handleResize)

  if (chartRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(chartRef.value)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  resizeObserver?.disconnect()
  disposeChart()
})
</script>

<template>
  <div class="chart-wrap">
    <div class="chart-toolbar">
      <button class="tool-btn" type="button" @click="exportCsv">
        <el-icon><Download /></el-icon>
        导出 CSV
      </button>
      <button
        v-if="normalized.type !== 'table' && normalized.type !== 'kpi'"
        class="tool-btn"
        type="button"
        @click="downloadChart"
      >
        <el-icon><Download /></el-icon>
        下载图表
      </button>
    </div>
    <div v-if="normalized.type === 'table'" class="table-wrap">
      <el-table :data="tableRows" stripe border size="small" max-height="360" empty-text="暂无数据">
        <el-table-column
          v-for="col in tableColumns"
          :key="col"
          :label="columnHeader(col, columnLabels)"
          min-width="120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ formatCell(row[col]) }}
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-else ref="chartRef" class="echart" />
  </div>
</template>

<style scoped>
.chart-wrap {
  width: 100%;
  min-height: 320px;
  margin-top: 12px;
}
.chart-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.tool-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  border-radius: 8px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}
.tool-btn:hover {
  border-color: var(--ds-accent);
  color: var(--ds-accent);
}
.echart {
  width: 100%;
  height: 320px;
}
.table-wrap {
  width: 100%;
}
</style>
