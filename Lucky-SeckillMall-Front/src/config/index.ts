/** 网关端口 */
export const GATEWAY_PORT = 8888

/** 网关基础地址 */
export const GATEWAY_BASE_URL = `http://localhost:${GATEWAY_PORT}`

/**
 * API 请求 baseURL
 * - 开发环境默认 ''，走 Vite 代理，避免浏览器跨域
 * - 生产环境默认网关地址，也可通过 VITE_API_BASE_URL 覆盖
 */
const envApiBase = import.meta.env.VITE_API_BASE_URL?.trim()

/** 开发环境留空走 Vite 代理；生产环境默认走网关 */
export const API_BASE_URL =
  envApiBase && envApiBase.length > 0
    ? envApiBase
    : import.meta.env.DEV
      ? ''
      : GATEWAY_BASE_URL
