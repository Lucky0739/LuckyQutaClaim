import QRCode from 'qrcode'

/** 判断 qrCode 是否已是可直接展示的图片地址 */
export function isQrCodeImageSource(content: string): boolean {
  return /^data:image\//i.test(content) || /^https?:\/\//i.test(content)
}

/** 将二维码内容渲染为可展示的 data URL */
export async function renderQrCodeDataUrl(content: string): Promise<string> {
  const trimmed = content.trim()
  if (!trimmed) {
    throw new Error('二维码内容为空')
  }
  if (isQrCodeImageSource(trimmed)) {
    return trimmed
  }
  return QRCode.toDataURL(trimmed, {
    width: 220,
    margin: 1,
    errorCorrectionLevel: 'M',
  })
}
