import type { LoginUserInfo } from '@/types/user'

const TOKEN_KEY = 'Authorization'
const USER_INFO_KEY = 'userInfo'

export const AUTH_CHANGED_EVENT = 'auth-changed'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  const value = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  localStorage.setItem(TOKEN_KEY, value)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUserInfo(): LoginUserInfo | null {
  const raw = localStorage.getItem(USER_INFO_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as LoginUserInfo
  } catch {
    return null
  }
}

export function setUserInfo(userInfo: LoginUserInfo): void {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
  window.dispatchEvent(new CustomEvent(AUTH_CHANGED_EVENT))
}

export function removeUserInfo(): void {
  localStorage.removeItem(USER_INFO_KEY)
}

export function clearAuth(): void {
  removeToken()
  removeUserInfo()
  window.dispatchEvent(new CustomEvent(AUTH_CHANGED_EVENT))
}

export function isLoggedIn(): boolean {
  return !!getToken() && !!getUserInfo()
}

export function hasRole(role: string): boolean {
  return getUserInfo()?.roles?.includes(role) ?? false
}

export function isAdmin(): boolean {
  return hasRole('admin')
}

export function isMerchant(): boolean {
  return hasRole('merchant')
}

export { TOKEN_KEY }

/** 登录时记住的手机号，用于注销账号等需发送验证码的场景 */
export const LOGIN_MOBILE_KEY = 'login_remember_mobile'

export function getStoredLoginMobile(): string {
  return localStorage.getItem(LOGIN_MOBILE_KEY) || ''
}
