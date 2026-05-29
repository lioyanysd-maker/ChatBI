import axios from 'axios'
import type { ApiResponse } from '../types/chatbi'
import type { ColumnSemanticItem } from '../types/semantic'

const http = axios.create({
  baseURL: '/api/chatbi/datasources',
  timeout: 30000,
})

function applyAuthHeaders() {
  const apiKey = import.meta.env.VITE_CHATBI_API_KEY as string | undefined
  if (apiKey) {
    http.defaults.headers.common['X-API-Key'] = apiKey
  }
}

applyAuthHeaders()

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(message))
  },
)

function unwrap<T>(data: ApiResponse<T>): T {
  if (data.code !== 200) {
    throw new Error(data.message)
  }
  return data.data
}

export async function fetchColumnSemantics(dataSourceId: number, table: string) {
  const { data } = await http.get<ApiResponse<ColumnSemanticItem[]>>(
    `/${dataSourceId}/semantics/columns`,
    { params: { table } },
  )
  return unwrap(data)
}

export async function updateColumnSemantics(
  dataSourceId: number,
  payload: {
    items: Array<{
      tableName: string
      columnName: string
      businessName?: string
      description?: string
    }>
  },
) {
  const { data } = await http.put<ApiResponse<ColumnSemanticItem[]>>(
    `/${dataSourceId}/semantics`,
    payload,
  )
  return unwrap(data)
}
