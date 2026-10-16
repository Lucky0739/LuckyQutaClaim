import type { PageResult } from '@/types/common'
import { isOrderPayExpired } from '@/utils/orderExpire'

export type OrderId = string
export type OrderNo = string

/** 订单状态（与后端 Order.status 一致） */
export const OrderStatus = {
  PENDING_PAYMENT: 0,
  PAID: 1,
  SHIPPED: 2,
  RECEIVED: 3,
  CANCELLED: 4,
  REFUNDED: 5,
  CLOSED: 6,
} as const
export type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus]

export interface AddOrderRequest {
  goodsId: string
  skuId?: string
  userAddressId: string
  buyGoodsNum: number
}

export interface OrderQueryRequest {
  orderNo?: string
  goodsName?: string
  status?: number | string
  pageNum?: number
  pageSize?: number
}

export interface OrderVO {
  id?: OrderId
  orderId?: OrderId
  orderNo: OrderNo
  goodsId?: string
  skuId?: string
  userAddressId?: string
  goodsName?: string
  goodsImg?: string
  skuSpecName?: string
  goodsNum?: number
  buyGoodsNum?: number
  goodsPrice?: number
  totalAmount?: number
  orderAmount?: number
  payAmount?: number
  status?: number
  statusDesc?: string
  receiver?: string
  mobile?: string
  province?: string
  city?: string
  area?: string
  detailAddress?: string
  address?: string
  createTime?: string
  updateTime?: string
  payTime?: string
  deliveryTime?: string
  receiveTime?: string
  cancelTime?: string
  /** 支付截止时间（超时未支付将自动关闭） */
  expireTime?: string
}

export type OrderRaw = Omit<
  OrderVO,
  | 'id'
  | 'orderId'
  | 'orderNo'
  | 'goodsId'
  | 'skuId'
  | 'userAddressId'
  | 'goodsPrice'
  | 'totalAmount'
  | 'orderAmount'
  | 'payAmount'
> & {
  id?: string | number
  orderId?: string | number
  orderNo?: string
  goodsId?: string | number
  skuId?: string | number
  userAddressId?: string | number
  goodsPrice?: number | string
  totalAmount?: number | string
  orderAmount?: number | string
  payAmount?: number | string
}

function toIdString(value?: string | number | null): string {
  if (value === undefined || value === null || value === '') return ''
  return String(value)
}

function toNumber(value?: number | string | null): number | undefined {
  if (value === undefined || value === null || value === '') return undefined
  const num = Number(value)
  return Number.isFinite(num) ? num : undefined
}

export function normalizeOrder(raw: OrderRaw): OrderVO {
  return {
    ...raw,
    id: toIdString(raw.id ?? raw.orderId),
    orderId: toIdString(raw.orderId ?? raw.id),
    orderNo: String(raw.orderNo ?? ''),
    goodsId: toIdString(raw.goodsId),
    skuId: toIdString(raw.skuId),
    userAddressId: toIdString(raw.userAddressId),
    goodsPrice: toNumber(raw.goodsPrice),
    totalAmount: toNumber(raw.totalAmount),
    orderAmount: toNumber(raw.orderAmount),
    payAmount: toNumber(raw.payAmount),
    goodsNum: raw.goodsNum ?? raw.buyGoodsNum ?? 0,
    buyGoodsNum: raw.buyGoodsNum ?? raw.goodsNum ?? 0,
  }
}

export function normalizeOrderList(records: OrderRaw[]): OrderVO[] {
  if (!Array.isArray(records)) return []
  return records.map(normalizeOrder).filter((item) => item.orderNo || item.id)
}

export function normalizeOrderPage(page?: PageResult<OrderRaw>): PageResult<OrderVO> {
  return {
    records: normalizeOrderList(page?.records ?? []),
    total: Number(page?.total ?? 0),
    pageNum: Number(page?.pageNum ?? 1),
    pageSize: Number(page?.pageSize ?? 10),
    totalPages: Number(page?.totalPages ?? 0),
  }
}

export function getOrderAmount(order: OrderVO): number | undefined {
  return order.payAmount ?? order.orderAmount ?? order.totalAmount
}

export function formatOrderAmount(value?: number | null): string {
  if (value == null || !Number.isFinite(value)) return '--'
  return value.toFixed(2)
}

export function formatOrderStatus(order: Pick<OrderVO, 'status' | 'statusDesc'>): string {
  if (order.statusDesc) return order.statusDesc
  const status = order.status
  if (status === OrderStatus.PENDING_PAYMENT) return '待支付'
  if (status === OrderStatus.PAID) return '待发货'
  if (status === OrderStatus.SHIPPED) return '已发货'
  if (status === OrderStatus.RECEIVED) return '已收货'
  if (status === OrderStatus.CANCELLED) return '已取消'
  if (status === OrderStatus.REFUNDED) return '已退款'
  if (status === OrderStatus.CLOSED) return '已过期'
  return status == null ? '--' : `状态 ${status}`
}

/** 待支付订单均可手动取消（超时未支付也走 cancel，后端置 status=4） */
export function canCancelOrder(order: Pick<OrderVO, 'status'>): boolean {
  return order.status === OrderStatus.PENDING_PAYMENT || order.status === undefined
}

export function canPayOrder(
  order: Pick<OrderVO, 'status' | 'orderNo' | 'expireTime' | 'createTime'>,
): boolean {
  return Boolean(order.orderNo)
    && order.status === OrderStatus.PENDING_PAYMENT
    && !isOrderPayExpired(order)
}

export function canDeliverOrder(order: Pick<OrderVO, 'status' | 'orderNo'>): boolean {
  return Boolean(order.orderNo) && order.status === OrderStatus.PAID
}

export function canConfirmReceipt(order: Pick<OrderVO, 'status' | 'orderNo'>): boolean {
  return Boolean(order.orderNo) && order.status === OrderStatus.SHIPPED
}

export function formatOrderAddress(order: Pick<OrderVO, 'province' | 'city' | 'area' | 'detailAddress' | 'address'>): string {
  if (order.address?.trim()) return order.address.trim()
  const parts = [order.province, order.city, order.area, order.detailAddress].filter(Boolean)
  return parts.join(' ') || '--'
}
