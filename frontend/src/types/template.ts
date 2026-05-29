export interface QueryTemplate {
  id: number
  dataSourceId?: number | null
  title: string
  question: string
  category: 'query' | 'chat'
  chartType: string
  sortOrder: number
}

export interface QueryTemplateForm {
  dataSourceId?: number | null
  title: string
  question: string
  category?: 'query' | 'chat'
  chartType?: string
  sortOrder?: number
}
