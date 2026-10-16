/** 行为验证码类型（对应 CaptchaTypeConstant） */
export const CaptchaType = {
  /** 滑块验证码 */
  SLIDER: 'SLIDER',
  /** 旋转验证码 */
  ROTATE: 'ROTATE',
  /** 滑动还原验证码 */
  CONCAT: 'CONCAT',
  /** 文字点选验证码 */
  WORD_IMAGE_CLICK: 'WORD_IMAGE_CLICK',
  /** 随机类型（由后端随机选择） */
  RANDOM: 'RANDOM',
} as const

export type CaptchaType = (typeof CaptchaType)[keyof typeof CaptchaType]

/** 天爱验证码成功状态码 */
export const CAPTCHA_SUCCESS_CODE = 200

/** 天爱验证码统一响应 ApiResponse<T> */
export interface CaptchaApiResponse<T = unknown> {
  code: number
  msg?: string
  data?: T
}

/** 滑动轨迹点 */
export interface CaptchaTrackPoint {
  x?: number
  y?: number
  t?: number
  type?: string
}

/** 行为轨迹 ImageCaptchaTrack */
export interface ImageCaptchaTrack {
  bgImageWidth?: number
  bgImageHeight?: number
  templateImageWidth?: number
  templateImageHeight?: number
  startTime?: number
  stopTime?: number
  left?: number
  top?: number
  trackList?: CaptchaTrackPoint[]
  data?: unknown
}

/** 生成验证码响应 ImageCaptchaVO */
export interface ImageCaptchaVO {
  id: string
  type: string
  backgroundImage: string
  templateImage?: string
  backgroundImageTag?: string
  templateImageTag?: string
  backgroundImageWidth?: number
  backgroundImageHeight?: number
  templateImageWidth?: number
  templateImageHeight?: number
  data?: unknown
}

/** 校验验证码请求 CaptchaData */
export interface CaptchaCheckRequest {
  /** 验证码 ID（gen 接口返回） */
  id: string
  /** 用户滑动/点选轨迹 */
  data: ImageCaptchaTrack
}

/** 校验成功响应 data: { id: captchaId } */
export interface CaptchaCheckResult {
  id: string
}

/** 判断天爱验证码响应是否成功 */
export function isCaptchaSuccess<T>(response: CaptchaApiResponse<T>): boolean {
  return response.code === CAPTCHA_SUCCESS_CODE
}
