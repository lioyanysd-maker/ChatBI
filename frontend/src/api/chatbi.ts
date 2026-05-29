import axios from 'axios'
import type { ApiResponse, HistoryPage, QueryResponse, SessionMessages, SessionSummary } from '../types/chatbi'

function buildAuthHeaders(): Record<string, string> {
  const headers: Record<string, string> = {}
  const apiKey = import.meta.env.VITE_CHATBI_API_KEY as string | undefined
  if (apiKey) {
    headers['X-API-Key'] = apiKey
  }
  return headers
}

const http = axios.create({
  baseURL: '/api/chatbi',
  timeout: 60000,
  headers: buildAuthHeaders(),
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(message))
  },
)

export async function queryChatBI(payload: {
  question: string
  sessionId?: string
  chartType?: string
  dataSourceId?: number | null
}) {
  const { data } = await http.post<ApiResponse<QueryResponse>>('/query', payload)
  if (data.code !== 200) {
    throw new Error(data.message)
  }
  return data.data
}

export async function executeSql(payload: {
  sql: string
  sessionId?: string
  chartType?: string
  dataSourceId?: number | null
  question?: string
}) {
  const { data } = await http.post<ApiResponse<QueryResponse>>('/execute-sql', payload)
  if (data.code !== 200) {
    throw new Error(data.message)
  }
  return data.data
}

export async function fetchSchema(dataSourceId?: number | null) {
  const { data } = await http.get<ApiResponse<{ tables: string[]; description: string }>>('/schema', {
    params: dataSourceId != null ? { dataSourceId } : undefined,
  })
  if (data.code !== 200) {
    throw new Error(data.message)
  }
  return data.data
}

export async function fetchHistory(sessionId: string, page = 1, size = 20) {
  const { data } = await http.get<ApiResponse<HistoryPage>>('/history', {
    params: { sessionId, page, size },
  })
  if (data.code !== 200) {
    throw new Error(data.message)
  }
  return data.data
}

export async function fetchSessions(limit = 30) {
  const { data } = await http.get<ApiResponse<SessionSummary[]>>('/sessions', {
    params: { limit },
  })
  if (data.code !== 200) {
    throw new Error(data.message)
  }
  return data.data
}

export async function fetchSessionMessages(sessionId: string) {
  const { data } = await http.get<ApiResponse<SessionMessages>>(`/sessions/${sessionId}/messages`)
  if (data.code !== 200) {
    throw new Error(data.message)
  }
  return data.data
}

export async function deleteSessions(sessionIds: string[]) {
  const { data } = await http.delete<ApiResponse<void>>('/sessions', {
    data: { sessionIds },
  })
  if (data.code !== 200) {
    throw new Error(data.message)
  }
}

export { queryChatBIStream } from './queryStream'
export type { QueryStreamHandlers } from './queryStream'
