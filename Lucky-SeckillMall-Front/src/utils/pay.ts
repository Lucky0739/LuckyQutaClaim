import { getPayUrl, hasPayUrl, type PayResultVO } from '@/types/pay'

/** 判断是否为支付宝 pageExecute 返回的 HTML 表单 */
export function isPayFormHtml(content: string): boolean {
  const trimmed = content.trim()
  if (/<form[\s>]/i.test(trimmed)) return true
  return trimmed.startsWith('<') && /<html[\s>]/i.test(trimmed)
}

/** 判断是否为可直接跳转的 http(s) 地址 */
export function isNavigablePayUrl(content: string): boolean {
  return /^https?:\/\//i.test(content.trim())
}

/** 判断是否为支付宝 GET 模式 gateway 链接（page.pay 不应直接 GET 打开，会 504） */
export function isAlipayGatewayGetUrl(content: string): boolean {
  return /openapi(?:-sandbox)?\.(?:dl\.)?alipay(dev)?\.com\/gateway\.do\?/i.test(content.trim())
}

/** 将支付宝 HTML 表单写入新窗口或当前页提交 */
export function submitPayFormHtml(html: string): void {
  const payWindow = window.open('', '_blank')
  if (payWindow) {
    payWindow.document.open()
    payWindow.document.write(html)
    payWindow.document.close()
    return
  }

  const container = document.createElement('div')
  container.style.display = 'none'
  container.innerHTML = html
  document.body.appendChild(container)
  const form = container.querySelector('form')
  if (!form) {
    document.body.removeChild(container)
    throw new Error('支付表单无效，请允许浏览器弹窗后重试')
  }
  form.submit()
  window.setTimeout(() => {
    document.body.removeChild(container)
  }, 3000)
}

/**
 * 打开支付宝沙箱收银台
 * payUrl 可能是 http 链接，也可能是 SDK 返回的 HTML 自动提交表单
 */
export function openSandboxPayPage(result: PayResultVO): void {
  const content = hasPayUrl(result) ? getPayUrl(result) : ''
  if (!content) {
    throw new Error('未获取到支付页面，请稍后重试')
  }

  if (isPayFormHtml(content)) {
    submitPayFormHtml(content)
    return
  }

  if (isNavigablePayUrl(content)) {
    if (isAlipayGatewayGetUrl(content)) {
      throw new Error(
        '支付链接格式不正确：后端需使用 alipayClient.pageExecute(request) 返回 HTML 表单（POST），不能使用 GET 模式生成 gateway.do 链接',
      )
    }
    const payWindow = window.open(content, '_blank', 'noopener,noreferrer')
    if (!payWindow) {
      window.location.href = content
    }
    return
  }

  throw new Error('支付页面格式无效，请稍后重试')
}

/** 轮询支付状态，直到成功或超时 */
export async function pollPayStatus(
  orderNo: string,
  options: { intervalMs?: number; maxAttempts?: number } = {},
): Promise<boolean> {
  const intervalMs = options.intervalMs ?? 2000
  const maxAttempts = options.maxAttempts ?? 15

  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const { queryPayStatus } = await import('@/api/pay')
    const paid = await queryPayStatus(orderNo)
    if (paid) return true
    if (attempt < maxAttempts - 1) {
      await new Promise((resolve) => window.setTimeout(resolve, intervalMs))
    }
  }
  return false
}
