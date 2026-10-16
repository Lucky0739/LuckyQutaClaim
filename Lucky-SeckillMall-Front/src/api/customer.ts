import { API_BASE_URL } from '@/config'
import { get } from '@/utils/request'
import { getToken, TOKEN_KEY } from '@/utils/auth'

const BASE = '/ai/customer'

/** 同步客服聊天 */
export function customerChat(message: string, chatId?: string) {
  return get<string>(`${BASE}/chat`, {
    params: {
      message,
      ...(chatId ? { chatId } : {}),
    },
  })
}

export interface StreamCustomerChatOptions {
  message: string
  chatId?: string
  signal?: AbortSignal
  onChunk: (chunk: string) => void
}

function buildStreamUrl(path: string, message: string, chatId?: string) {
  const params = new URLSearchParams({ message })
  if (chatId) params.set('chatId', chatId)
  return `${API_BASE_URL}${BASE}${path}?${params.toString()}`
}

function buildAuthHeaders() {
  const headers: Record<string, string> = {
    Accept: 'text/event-stream',
  }
  const token = getToken()
  if (token) {
    headers[TOKEN_KEY] = token
  }
  return headers
}

function extractSseData(rawEvent: string): string {
  const lines = rawEvent.split('\n')
  const dataLines = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trimStart())
  return dataLines.join('\n')
}

async function readSseStream(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  onChunk: (chunk: string) => void,
) {
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split('\n\n')
    buffer = events.pop() ?? ''

    for (const event of events) {
      const data = extractSseData(event)
      if (data && data !== '[DONE]') {
        onChunk(data)
      }
    }
  }

  if (buffer.trim()) {
    const data = extractSseData(buffer)
    if (data && data !== '[DONE]') {
      onChunk(data)
    }
  }
}

/** SSE 流式客服聊天 */
export async function streamCustomerChat(options: StreamCustomerChatOptions): Promise<void> {
  const { message, chatId, signal, onChunk } = options

  const response = await fetch(buildStreamUrl('/chat/sse', message, chatId), {
    headers: buildAuthHeaders(),
    signal,
  })

  if (!response.ok) {
    let errMsg = '客服连接失败，请稍后重试'
    try {
      const json = (await response.json()) as { message?: string }
      errMsg = json.message || errMsg
    } catch {
      // ignore parse error
    }
    throw new Error(errMsg)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('无法读取客服响应')
  }

  try {
    await readSseStream(reader, onChunk)
  } finally {
    reader.releaseLock()
  }
}
