import { ORDER_PAY_TIMEOUT_MINUTES } from '@/constants/order'
import { OrderStatus, type OrderVO } from '@/types/order'

function parseDateTime(value?: string): Date | null {
  if (!value?.trim()) return null
  const normalized = value.trim().replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

/** 解析订单支付截止时间（优先 expireTime，否则 createTime + 15 分钟） */
export function resolveOrderExpireAt(
  order: Pick<OrderVO, 'expireTime' | 'createTime'>,
): Date | null {
  const expireAt = parseDateTime(order.expireTime)
  if (expireAt) return expireAt

  const createdAt = parseDateTime(order.createTime)
  if (!createdAt) return null

  return new Date(createdAt.getTime() + ORDER_PAY_TIMEOUT_MINUTES * 60 * 1000)
}

/** 是否待支付且已超过支付时限 */
export function isOrderPayExpired(
  order: Pick<OrderVO, 'status' | 'expireTime' | 'createTime'>,
): boolean {
  if (order.status !== OrderStatus.PENDING_PAYMENT) return false
  const expireAt = resolveOrderExpireAt(order)
  if (!expireAt) return false
  return Date.now() >= expireAt.getTime()
}

/** 剩余支付时间（毫秒） */
export function getOrderPayRemainMs(
  order: Pick<OrderVO, 'status' | 'expireTime' | 'createTime'>,
): number {
  if (order.status !== OrderStatus.PENDING_PAYMENT) return 0
  const expireAt = resolveOrderExpireAt(order)
  if (!expireAt) return ORDER_PAY_TIMEOUT_MINUTES * 60 * 1000
  return Math.max(0, expireAt.getTime() - Date.now())
}

/** 格式化为 MM:SS 或 HH:MM:SS */
export function formatPayCountdown(remainMs: number): string {
  const totalSec = Math.max(0, Math.ceil(remainMs / 1000))
  const hours = Math.floor(totalSec / 3600)
  const minutes = Math.floor((totalSec % 3600) / 60)
  const seconds = totalSec % 60
  const pad = (n: number) => String(n).padStart(2, '0')
  if (hours > 0) return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
  return `${pad(minutes)}:${pad(seconds)}`
}

export function isPendingPaymentOrder(order: Pick<OrderVO, 'status'>): boolean {
  return order.status === OrderStatus.PENDING_PAYMENT
}
