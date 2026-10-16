import type { OrderNo } from '@/types/order'

export type PaymentMethod = 'alipay' | 'balance'

/** 沙箱支付结果，对应后端 PayResultVO */
export interface PayResultVO {
  orderNo?: OrderNo
  /** 支付宝收银台：可能是跳转 URL，也可能是 HTML 自动提交表单 */
  payUrl?: string
  /** 扫码支付二维码内容（可选） */
  qrCode?: string
  /** 支付宝交易号（支付完成后可能有值） */
  tradeNo?: string
}

/** 从 PayResultVO 提取可用的支付跳转地址 */
export function getPayUrl(result: PayResultVO): string {
  const payUrl = result.payUrl?.trim()
  if (payUrl) return payUrl
  throw new Error('未获取到支付链接，请稍后重试')
}

export function hasPayUrl(result: PayResultVO): boolean {
  return Boolean(result.payUrl?.trim())
}

export function hasQrCode(result: PayResultVO): boolean {
  return Boolean(result.qrCode?.trim())
}

export type PayPresentation = 'url' | 'qrcode' | 'both'

/** 判断后端返回的支付方式 */
export function getPayPresentation(result: PayResultVO): PayPresentation {
  const url = hasPayUrl(result)
  const qr = hasQrCode(result)
  if (url && qr) return 'both'
  if (qr) return 'qrcode'
  if (url) return 'url'
  throw new Error('未获取到支付方式，请稍后重试')
}
