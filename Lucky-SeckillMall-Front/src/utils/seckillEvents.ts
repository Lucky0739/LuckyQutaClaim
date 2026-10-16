export const SECKILL_DISPLAY_CHANGED = 'seckill:display-changed'

/** 秒杀商品审核通过等变更后通知秒杀页立即刷新 */
export function notifySeckillDisplayChanged() {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent(SECKILL_DISPLAY_CHANGED))
}
