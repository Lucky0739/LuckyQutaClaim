import { closeExpiredOrder } from '@/api/order'
import { OrderStatus, type OrderVO } from '@/types/order'
import { isOrderPayExpired } from '@/utils/orderExpire'

const closingOrderNos = new Set<string>()

type ExpirableOrder = Pick<OrderVO, 'orderNo' | 'status' | 'expireTime' | 'createTime'>

/** 调用后端关闭超时未支付订单 POST /order/close/expired */
export async function closeOrderIfExpired(order: ExpirableOrder): Promise<boolean> {
  const orderNo = order.orderNo?.trim()
  if (!orderNo) return false
  if (order.status !== OrderStatus.PENDING_PAYMENT) return false
  if (!isOrderPayExpired(order)) return false
  if (closingOrderNos.has(orderNo)) return false

  closingOrderNos.add(orderNo)
  try {
    await closeExpiredOrder(orderNo)
    return true
  } catch {
    return false
  } finally {
    closingOrderNos.delete(orderNo)
  }
}

/** 扫描列表中的待支付订单，超时则关闭并触发刷新 */
export async function scanAndCloseExpiredOrders(
  orders: ExpirableOrder[],
  onClosed?: () => void | Promise<void>,
): Promise<void> {
  let anyClosed = false
  for (const order of orders) {
    if (!isOrderPayExpired(order)) continue
    if (await closeOrderIfExpired(order)) {
      anyClosed = true
    }
  }
  if (anyClosed) {
    await onClosed?.()
  }
}
