import axios from 'axios'
import type { ApiResponse } from '../types/chatbi'
import type { DataSource, DataSourceForm, TableWhitelistItem } from '../types/datasource'

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

export async function fetchDataSources() {
  const { data } = await http.get<ApiResponse<DataSource[]>>('')
  return unwrap(data)
}

export async function createDataSource(form: DataSourceForm) {
  const { data } = await http.post<ApiResponse<DataSource>>('', form)
  return unwrap(data)
}

export async function updateDataSource(id: number, form: DataSourceForm) {
  const { data } = await http.put<ApiResponse<DataSource>>(`/${id}`, form)
  return unwrap(data)
}

export async function deleteDataSource(id: number) {
  const { data } = await http.delete<ApiResponse<void>>(`/${id}`)
  unwrap(data)
}

export async function testDataSourceConnection(form: DataSourceForm) {
  const { data } = await http.post<ApiResponse<{ success: boolean; message: string }>>('/test', form)
  return unwrap(data)
}

export async function fetchTableWhitelist(dataSourceId: number) {
  const { data } = await http.get<ApiResponse<TableWhitelistItem[]>>(`/${dataSourceId}/whitelist`)
  return unwrap(data)
}

export async function updateTableWhitelist(
  dataSourceId: number,
  payload: { items: Array<{ tableName: string; tableComment?: string; active: boolean }> },
) {
  const { data } = await http.put<ApiResponse<TableWhitelistItem[]>>(`/${dataSourceId}/whitelist`, payload)
  return unwrap(data)
}
