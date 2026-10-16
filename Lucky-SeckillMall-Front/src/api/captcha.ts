import { postCaptcha } from '@/utils/captchaRequest'
import type {
  CaptchaCheckRequest,
  CaptchaCheckResult,
  CaptchaType,
  ImageCaptchaVO,
} from '@/types/captcha'

const BASE = '/captcha'

/**
 * 生成行为验证码
 * POST /captcha/gen?type=SLIDER|ROTATE|CONCAT|WORD_IMAGE_CLICK|RANDOM
 * @param type 验证码类型，不传默认 SLIDER；传 RANDOM 由后端随机选择
 */
export function genCaptcha(type?: CaptchaType) {
  return postCaptcha<ImageCaptchaVO>(`${BASE}/gen`, null, {
    params: type ? { type } : undefined,
  })
}

/**
 * 校验行为验证码
 * POST /captcha/check
 * @returns 校验成功返回 captchaId，可用于后续发送短信等接口
 */
export function checkCaptcha(data: CaptchaCheckRequest) {
  return postCaptcha<CaptchaCheckResult>(`${BASE}/check`, data)
}
