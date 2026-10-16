import request from '@/utils/request'
import type { ApiResponse } from '@/types/common'
import type { OrderNo } from '@/types/order'
import type { PayResultVO } from '@/types/pay'

const BASE = '/pay'

function unwrap<T>(response: { data: ApiResponse<T> }): T {
  const res = response.data
  if (res.success === false) {
    throw new Error(res.message || '支付请求失败')
  }
  return res.data as T
}

/**
 * 创建沙箱支付（跳转支付宝沙箱收银台）
 * POST /pay/sandbox?orderNo=
 */
export async function createSandboxPay(orderNo: OrderNo): Promise<PayResultVO> {
  const response = await request.post(`${BASE}/sandbox`, null, {
    params: { orderNo },
    timeout: 60000,
  })
  return unwrap<PayResultVO>(response)
}

/**
 * 余额支付
 * POST /pay/balance?orderNo=
 */
export async function balancePay(orderNo: OrderNo): Promise<PayResultVO> {
  const response = await request.post(`${BASE}/balance`, null, { params: { orderNo } })
  return unwrap<PayResultVO>(response)
}

/**
 * 查询支付是否成功
 * POST /pay/query?orderNo=
 */
export async function queryPayStatus(orderNo: OrderNo): Promise<boolean> {
  const response = await request.post(`${BASE}/query`, null, { params: { orderNo } })
  return unwrap<boolean>(response) ?? false
}

/**
 * 支付成功回调（确认支付并扣减库存）
 * POST /pay/callback?orderNo=
 */
export async function handlePaySuccess(orderNo: OrderNo): Promise<boolean> {
  const response = await request.post(`${BASE}/callback`, null, { params: { orderNo } })
  return unwrap<boolean>(response) ?? false
}
