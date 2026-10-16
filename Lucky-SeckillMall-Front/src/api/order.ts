import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/common'
import type { AddOrderRequest, OrderId, OrderNo, OrderQueryRequest, OrderRaw, OrderVO } from '@/types/order'
import { normalizeOrder, normalizeOrderPage } from '@/types/order'
import { parseJsonPreservingLongIds } from '@/utils/json'

const BASE = '/order'
const LONG_FIELDS = ['id', 'orderId', 'goodsId', 'skuId', 'userId', 'userAddressId', 'data']

export async function addOrder(data: AddOrderRequest): Promise<OrderId> {
  const response = await request.post(`${BASE}/add`, data, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<string | number>>(raw, ['data'])
      } catch {
        return JSON.parse(raw)
      }
    }],
  })
  const res = response.data as ApiResponse<string | number>
  if (res.success === false) throw new Error(res.message || '创建订单失败')
  return String(res.data ?? '')
}

/** 取消订单 POST /order/cancel?orderNo=（取消后 status=4 已取消） */
export async function cancelOrder(orderNo: OrderNo): Promise<boolean> {
  const response = await request.post(`${BASE}/cancel`, null, {
    params: { orderNo },
  })
  const res = response.data as ApiResponse<boolean>
  if (res.success === false) throw new Error(res.message || '取消订单失败')
  return res.data ?? true
}

export async function getOrderByOrderNo(orderNo: OrderNo): Promise<OrderVO> {
  const response = await request.get(`${BASE}/query`, {
    params: { orderNo },
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<OrderRaw>>(raw, LONG_FIELDS)
      } catch {
        return JSON.parse(raw)
      }
    }],
  })
  const res = response.data as ApiResponse<OrderRaw>
  if (res.success === false) throw new Error(res.message || '查询订单失败')
  return normalizeOrder(res.data ?? { orderNo })
}

export async function getOrderVOByOrderNo(orderNo: OrderNo): Promise<OrderVO> {
  const response = await request.get(`${BASE}/query/vo`, {
    params: { orderNo },
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<OrderRaw>>(raw, LONG_FIELDS)
      } catch {
        return JSON.parse(raw)
      }
    }],
  })
  const res = response.data as ApiResponse<OrderRaw>
  if (res.success === false) throw new Error(res.message || '查询订单失败')
  return normalizeOrder(res.data ?? { orderNo })
}

export async function getOrderById(orderId: OrderId): Promise<OrderVO> {
  const response = await request.get(`${BASE}/get/${orderId}`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<OrderRaw>>(raw, LONG_FIELDS)
      } catch {
        return JSON.parse(raw)
      }
    }],
  })
  const res = response.data as ApiResponse<OrderRaw>
  if (res.success === false) throw new Error(res.message || '查询订单失败')
  return normalizeOrder(res.data ?? { id: orderId })
}

export async function getOrderList(data: OrderQueryRequest = {}): Promise<PageResult<OrderVO>> {
  const payload = {
    pageNum: data.pageNum ?? 1,
    pageSize: data.pageSize ?? 10,
    ...(data.orderNo ? { orderNo: data.orderNo } : {}),
    ...(data.goodsName ? { goodsName: data.goodsName } : {}),
    ...(data.status !== '' && data.status !== undefined ? { status: Number(data.status) } : {}),
  }
  const response = await request.post(`${BASE}/list`, payload, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<OrderRaw>>>(raw, LONG_FIELDS)
      } catch {
        return JSON.parse(raw)
      }
    }],
  })
  const res = response.data as ApiResponse<PageResult<OrderRaw>>
  if (res.success === false) throw new Error(res.message || '加载订单列表失败')
  return normalizeOrderPage(res.data)
}

export async function updateOrderStatus(orderNo: OrderNo, status: number): Promise<boolean> {
  const response = await request.post(`${BASE}/updateStatus`, null, { params: { orderNo, status } })
  const res = response.data as ApiResponse<boolean>
  if (res.success === false) throw new Error(res.message || '更新订单状态失败')
  return res.data ?? true
}

export async function closeExpiredOrder(orderNo: OrderNo): Promise<boolean> {
  const response = await request.post(`${BASE}/close/expired`, null, { params: { orderNo } })
  const res = response.data as ApiResponse<boolean>
  if (res.success === false) throw new Error(res.message || '关闭过期订单失败')
  return res.data ?? true
}

/** 商家发货 POST /order/deliver?orderNo= */
export async function deliverOrder(orderNo: OrderNo): Promise<boolean> {
  const response = await request.post(`${BASE}/deliver`, null, { params: { orderNo } })
  const res = response.data as ApiResponse<boolean>
  if (res.success === false) throw new Error(res.message || '发货失败')
  return res.data ?? true
}

/** 用户确认收货 POST /order/receive?orderNo= */
export async function confirmReceipt(orderNo: OrderNo): Promise<boolean> {
  const response = await request.post(`${BASE}/receive`, null, { params: { orderNo } })
  const res = response.data as ApiResponse<boolean>
  if (res.success === false) throw new Error(res.message || '确认收货失败')
  return res.data ?? true
}

/** 商家查询订单列表 POST /order/merchant/list */
export async function getMerchantOrderList(data: OrderQueryRequest = {}): Promise<PageResult<OrderVO>> {
  const payload = {
    pageNum: data.pageNum ?? 1,
    pageSize: data.pageSize ?? 10,
    ...(data.orderNo ? { orderNo: data.orderNo } : {}),
    ...(data.goodsName ? { goodsName: data.goodsName } : {}),
    ...(data.status !== '' && data.status !== undefined ? { status: Number(data.status) } : {}),
  }
  const response = await request.post(`${BASE}/merchant/list`, payload, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<OrderRaw>>>(raw, LONG_FIELDS)
      } catch {
        return JSON.parse(raw)
      }
    }],
  })
  const res = response.data as ApiResponse<PageResult<OrderRaw>>
  if (res.success === false) throw new Error(res.message || '加载商家订单失败')
  return normalizeOrderPage(res.data)
}
