import axios, { type AxiosRequestConfig } from 'axios'
import { API_BASE_URL } from '@/config'
import {
  CAPTCHA_SUCCESS_CODE,
  type CaptchaApiResponse,
  isCaptchaSuccess,
} from '@/types/captcha'

/**
 * 验证码专用 axios 实例
 * 天爱验证码 ApiResponse 与业务 Response 结构不同，需单独处理
 */
const captchaRequest = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/** 解析天爱验证码响应，失败时抛出 Error */
export function unwrapCaptcha<T>(response: CaptchaApiResponse<T>): T {
  if (!isCaptchaSuccess(response)) {
    throw new Error(response.msg || '验证码请求失败')
  }
  return response.data as T
}

/** POST 验证码接口 */
export async function postCaptcha<T>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig,
): Promise<T> {
  const response = await captchaRequest.post<CaptchaApiResponse<T>>(url, data, config)
  return unwrapCaptcha(response.data)
}

export { CAPTCHA_SUCCESS_CODE, captchaRequest }
