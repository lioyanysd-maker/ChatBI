import axios from 'axios'
import type { ApiResponse } from '../types/chatbi'
import type { QueryTemplate, QueryTemplateForm } from '../types/template'

const http = axios.create({
  baseURL: '/api/chatbi/templates',
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

export async function fetchQueryTemplates(dataSourceId?: number | null) {
  const { data } = await http.get<ApiResponse<QueryTemplate[]>>('', {
    params: dataSourceId != null ? { dataSourceId } : undefined,
  })
  return unwrap(data)
}

export async function createQueryTemplate(form: QueryTemplateForm) {
  const { data } = await http.post<ApiResponse<QueryTemplate>>('', form)
  return unwrap(data)
}

export async function deleteQueryTemplate(id: number) {
  const { data } = await http.delete<ApiResponse<void>>(`/${id}`)
  unwrap(data)
}
