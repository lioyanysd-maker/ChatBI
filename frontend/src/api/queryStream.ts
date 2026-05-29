import type { ChartData, QueryResponse } from '../types/chatbi'

export interface QueryStreamHandlers {
  onStep?: (step: string) => void
  onSql?: (sql: string) => void
  onChart?: (chart: ChartData) => void
  onDelta?: (text: string) => void
  onDone?: (result: QueryResponse) => void
  onError?: (message: string) => void
}

function buildAuthHeaders(): Record<string, string> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const apiKey = import.meta.env.VITE_CHATBI_API_KEY as string | undefined
  if (apiKey) {
    headers['X-API-Key'] = apiKey
  }
  return headers
}

function dispatchStreamEvent(event: string, data: string, handlers: QueryStreamHandlers) {
  switch (event) {
    case 'step':
      handlers.onStep?.(data)
      break
    case 'sql':
      handlers.onSql?.(data)
      break
    case 'chart':
      handlers.onChart?.(JSON.parse(data) as ChartData)
      break
    case 'delta':
      handlers.onDelta?.(data)
      break
    case 'done':
      handlers.onDone?.(JSON.parse(data) as QueryResponse)
      break
    case 'error':
      handlers.onError?.(data)
      break
    default:
      break
  }
}

function parseSseBuffer(buffer: string, handlers: QueryStreamHandlers): string {
  const parts = buffer.split('\n\n')
  const remainder = parts.pop() ?? ''
  for (const part of parts) {
    if (!part.trim()) {
      continue
    }
    let event = 'message'
    let data = ''
    for (const line of part.split('\n')) {
      if (line.startsWith('event:')) {
        event = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        data += line.slice(5).trim()
      }
    }
    if (data) {
      try {
        dispatchStreamEvent(event, data, handlers)
      } catch (error) {
        handlers.onError?.(error instanceof Error ? error.message : '流式响应解析失败')
      }
    }
  }
  return remainder
}

export async function queryChatBIStream(
  payload: {
    question: string
    sessionId?: string
    chartType?: string
    dataSourceId?: number | null
  },
  handlers: QueryStreamHandlers,
  signal?: AbortSignal,
) {
  const response = await fetch('/api/chatbi/query/stream', {
    method: 'POST',
    headers: buildAuthHeaders(),
    body: JSON.stringify(payload),
    signal,
  })

  if (!response.ok) {
    let message = `请求失败 (${response.status})`
    try {
      const json = await response.json()
      message = json.message || message
    } catch {
      // ignore
    }
    throw new Error(message)
  }

  if (!response.body) {
    throw new Error('服务器未返回流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    buffer = parseSseBuffer(buffer, handlers)
  }

  if (buffer.trim()) {
    parseSseBuffer(`${buffer}\n\n`, handlers)
  }
}
