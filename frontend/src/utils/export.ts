import type { ChartData } from '../types/chatbi'
import { columnHeader, formatCell, normalizeChartData } from './chartData'

function escapeCsvCell(value: unknown): string {
  const text = formatCell(value)
  if (/[",\n\r]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

export function exportChartDataCsv(chartData: ChartData, filename = 'chatbi-export.csv') {
  const data = normalizeChartData(chartData)
  const columns = data.columns ?? []
  const rows = data.rows ?? []
  const labels = data.columnLabels

  const header = columns.map((col) => escapeCsvCell(columnHeader(col, labels))).join(',')
  const body = rows.map((row) => columns.map((col) => escapeCsvCell(row[col])).join(',')).join('\n')
  const csv = `\uFEFF${header}\n${body}`

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

export async function copyText(text: string) {
  await navigator.clipboard.writeText(text)
}
