import { GATEWAY_BASE_URL } from '@/config'

/** 将后端返回的相对路径转为可访问的完整 URL */
export function resolveFileUrl(url?: string | null): string {
  if (!url?.trim()) return ''
  const trimmed = url.trim()
  if (/^https?:\/\//i.test(trimmed)) return trimmed
  if (trimmed.startsWith('//')) return `https:${trimmed}`
  const base = GATEWAY_BASE_URL.replace(/\/$/, '')
  return trimmed.startsWith('/') ? `${base}${trimmed}` : `${base}/${trimmed}`
}
