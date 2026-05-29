import type { ChartData } from '../types/chatbi'

/** 兼容后端 Jackson 可能把 xAxis 序列化为 xaxis 的情况 */
export function normalizeChartData(raw: ChartData | Record<string, unknown>): ChartData {
  const data = raw as Record<string, unknown>
  const xAxis = data.xAxis ?? data.xaxis

  let columns = data.columns as string[] | undefined
  const rows = (data.rows ?? []) as Record<string, unknown>[]

  if ((!columns || columns.length === 0) && rows.length > 0) {
    columns = Object.keys(rows[0])
  }

  return {
    type: String(data.type ?? 'table'),
    xAxis,
    series: data.series,
    unit: data.unit as string | undefined,
    columns,
    columnLabels: data.columnLabels as Record<string, string> | undefined,
    rows,
  }
}

export function columnHeader(
  key: string,
  columnLabels?: Record<string, string>,
): string {
  return columnLabels?.[key] ?? key
}

export function formatCell(value: unknown): string {
  if (value == null) return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}
