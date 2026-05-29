export interface ChartData {
  type: string
  xAxis?: unknown
  series?: unknown
  unit?: string
  columns?: string[]
  columnLabels?: Record<string, string>
  rows?: Record<string, unknown>[]
}

export interface QueryResponse {
  sessionId?: string
  question: string
  answerType?: 'query' | 'chat'
  generatedSql?: string
  chartData?: ChartData
  naturalExplanation: string
  rowCount: number
  executionTimeMs: number
  rawRows?: Record<string, unknown>[]
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface HistoryRecord {
  id: number
  sessionId: string
  question: string
  generatedSql: string
  executedSql: string
  rowCount: number
  executionTimeMs: number
  chartType: string
  createdAt: string
}

export interface HistoryPage {
  records: HistoryRecord[]
  total: number
  current: number
  size: number
}

export interface SessionSummary {
  sessionId: string
  title: string
  updatedAt: string
  messageCount: number
  dataSourceId?: number | null
}

export interface SessionMessages {
  sessionId: string
  dataSourceId?: number | null
  messages: ChatMessage[]
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  answerType?: 'query' | 'chat'
  sql?: string
  rowCount?: number
  executionTimeMs?: number
  chartData?: ChartData
}
