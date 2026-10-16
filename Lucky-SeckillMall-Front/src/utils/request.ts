import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { API_BASE_URL } from '@/config'
import type { ApiResponse } from '@/types/common'
import { isNotLoginResponse } from '@/types/common'
import { clearAuth, getToken, TOKEN_KEY } from '@/utils/auth'

const request: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/** 请求拦截：自动携带 Sa-Token */
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers[TOKEN_KEY] = token
  }
  // FormData 需由浏览器自动设置 multipart boundary，不能沿用默认 application/json
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

/** 响应拦截：统一处理业务错误 */
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    if (res && res.success === false) {
      if (isNotLoginResponse(res)) {
        clearAuth()
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return response
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      clearAuth()
    }
    const message =
      error.response?.data?.message ||
      error.message ||
      '网络异常，请稍后重试'
    return Promise.reject(new Error(message))
  },
)

/** 提取响应 data 字段 */
export function unwrap<T>(response: AxiosResponse<ApiResponse<T>>): T {
  return response.data.data as T
}

/** GET 请求 */
export function get<T>(url: string, config?: AxiosRequestConfig) {
  return request.get<ApiResponse<T>>(url, config).then(unwrap)
}

/** POST JSON 请求 */
export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return request.post<ApiResponse<T>>(url, data, config).then(unwrap)
}

/** POST 表单 / 文件上传 */
export function postForm<T>(url: string, data: FormData, config?: AxiosRequestConfig) {
  return request.post<ApiResponse<T>>(url, data, config).then(unwrap)
}

/** POST 请求（返回完整 Response，data 可能为空） */
export function postVoid(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return request.post<ApiResponse>(url, data, config).then(() => undefined)
}

/** PUT 请求 */
export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  const body = data === undefined || data === null ? undefined : data
  return request.put<ApiResponse<T>>(url, body, config).then(unwrap)
}

/** PUT 请求（返回完整 Response，data 可能为空） */
export function putVoid(url: string, data?: unknown, config?: AxiosRequestConfig) {
  const body = data === undefined || data === null ? undefined : data
  return request.put<ApiResponse>(url, body, config).then(() => undefined)
}

/** DELETE 请求（返回完整 Response，data 可能为空） */
export function delVoid(url: string, config?: AxiosRequestConfig) {
  return request.delete<ApiResponse>(url, config).then(() => undefined)
}

export default request
