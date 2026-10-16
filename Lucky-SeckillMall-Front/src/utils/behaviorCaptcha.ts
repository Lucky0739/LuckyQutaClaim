import { CaptchaType, type CaptchaType as CaptchaTypeValue } from '@/types/captcha'

const TAC_BASE = '/tac/tac'
const LOADER_SRC = '/tac/load.js'
const OVERLAY_ID = 'behavior-captcha-overlay'
const CONTAINER_ID = 'behavior-captcha-box'

export interface ShowBehaviorCaptchaOptions {
  /**
   * 验证码类型，默认 RANDOM（后端随机：滑块/旋转/滑动还原/文字点选）
   * @see CaptchaType
   */
  type?: CaptchaTypeValue
}

/** 用户主动关闭验证码弹窗 */
export class CaptchaCancelledError extends Error {
  constructor() {
    super('已取消验证')
    this.name = 'CaptchaCancelledError'
  }
}

let loadScriptPromise: Promise<void> | null = null

function loadTacLoader(): Promise<void> {
  if (window.loadTAC) return Promise.resolve()

  if (!loadScriptPromise) {
    loadScriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = LOADER_SRC
      script.onload = () => resolve()
      script.onerror = () => reject(new Error('验证码组件加载失败'))
      document.head.appendChild(script)
    })
  }

  return loadScriptPromise
}

function buildGenUrl(type: CaptchaTypeValue) {
  return `/captcha/gen?type=${encodeURIComponent(type)}`
}

/** 隐藏遮罩并清空验证码 DOM */
function hideCaptchaOverlay() {
  const overlay = document.getElementById(OVERLAY_ID)
  const container = document.getElementById(CONTAINER_ID)
  if (container) container.innerHTML = ''
  overlay?.classList.remove('is-active')
}

function ensureCaptchaContainer(): HTMLElement {
  let overlay = document.getElementById(OVERLAY_ID)
  if (!overlay) {
    overlay = document.createElement('div')
    overlay.id = OVERLAY_ID
    const container = document.createElement('div')
    container.id = CONTAINER_ID
    overlay.appendChild(container)
    document.body.appendChild(overlay)
  }

  let container = document.getElementById(CONTAINER_ID)
  if (!container) {
    container = document.createElement('div')
    container.id = CONTAINER_ID
    overlay.appendChild(container)
  }

  container.innerHTML = ''
  overlay.classList.add('is-active')
  return container
}

function finishCaptcha(tac: TacInstance) {
  tac.destroyWindow()
  hideCaptchaOverlay()
}

/**
 * 弹出行为验证码，校验成功后返回 captchaId（用于发送短信等二次校验）
 * @param options.type 验证码类型，默认 RANDOM
 */
export async function showBehaviorCaptcha(
  options: ShowBehaviorCaptchaOptions = {},
): Promise<string> {
  const captchaType = options.type ?? CaptchaType.RANDOM
  const genUrl = buildGenUrl(captchaType)

  await loadTacLoader()

  if (!window.loadTAC) {
    throw new Error('验证码组件加载失败')
  }

  ensureCaptchaContainer()

  return new Promise((resolve, reject) => {
    const captchaConfig: TacCaptchaConfig = {
      requestCaptchaDataUrl: genUrl,
      validCaptchaUrl: '/captcha/check',
      bindEl: `#${CONTAINER_ID}`,
      validSuccess: (res, _c, tac) => {
        finishCaptcha(tac)
        const captchaId = res.data?.id
        if (!captchaId) {
          reject(new Error('验证码校验结果异常'))
          return
        }
        resolve(captchaId)
      },
      validFail: (_res, _c, tac) => {
        tac.reloadCaptcha()
      },
      btnRefreshFun: (_el, tac) => {
        tac.reloadCaptcha()
      },
      btnCloseFun: (_el, tac) => {
        finishCaptcha(tac)
        reject(new CaptchaCancelledError())
      },
    }

    window
      .loadTAC!(TAC_BASE, captchaConfig, {})
      .then((tac) => tac.init())
      .catch((err) => {
        hideCaptchaOverlay()
        reject(err instanceof Error ? err : new Error('验证码初始化失败'))
      })
  })
}
