/** 天爱行为验证码 Web SDK 全局类型 */
interface TacInstance {
  init: () => void
  destroyWindow: () => void
  reloadCaptcha: () => void
}

interface TacCaptchaConfig {
  requestCaptchaDataUrl: string
  validCaptchaUrl: string
  bindEl: string
  validSuccess: (res: { data: { id: string } }, c: unknown, tac: TacInstance) => void
  validFail?: (res: unknown, c: unknown, tac: TacInstance) => void
  btnRefreshFun?: (el: unknown, tac: TacInstance) => void
  btnCloseFun?: (el: unknown, tac: TacInstance) => void
}

interface Window {
  loadTAC?: (
    url: string,
    config: TacCaptchaConfig,
    style?: Record<string, unknown>,
  ) => Promise<TacInstance>
}
