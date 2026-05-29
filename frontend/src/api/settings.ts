import axios from 'axios'
import type { ApiResponse } from '../types/chatbi'
import type { LlmSettings, LlmSettingsForm } from '../types/settings'

const http = axios.create({
  baseURL: '/api/chatbi/settings',
  timeout: 60000,
})

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

export async function fetchLlmSettings() {
  const { data } = await http.get<ApiResponse<LlmSettings>>('/llm')
  return unwrap(data)
}

export async function updateLlmSettings(form: LlmSettingsForm) {
  const { data } = await http.put<ApiResponse<LlmSettings>>('/llm', form)
  return unwrap(data)
}

export async function testLlmSettings(form: LlmSettingsForm) {
  const { data } = await http.post<ApiResponse<{ success: boolean; message: string }>>('/llm/test', form)
  return unwrap(data)
}
