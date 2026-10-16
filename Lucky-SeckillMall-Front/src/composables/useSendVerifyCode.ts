import { onMounted, onUnmounted, ref, watch } from 'vue'
import { sendVerifyCode } from '@/api/user'
import type { CaptchaType } from '@/types/captcha'
import type { VerifyCodeType } from '@/types/user'
import { CaptchaCancelledError, showBehaviorCaptcha } from '@/utils/behaviorCaptcha'

const MOBILE_PATTERN = /^1[3-9]\d{9}$/
const COUNTDOWN_SECONDS = 60
const EXPIRE_STORAGE_PREFIX = 'sms_verify_code_expire'
const MOBILE_STORAGE_PREFIX = 'sms_verify_code_mobile'

export interface UseSendVerifyCodeOptions {
  /** 获取当前手机号 */
  getMobile: () => string
  /** 验证码场景：1=注册，2=登录 */
  type: VerifyCodeType
  /** 行为验证码类型，默认 RANDOM（多种验证方式随机） */
  captchaType?: CaptchaType
  /** 恢复未过期的发送会话（如刷新后回填手机号） */
  onRestoreSession?: (mobile: string) => void
  /** 校验/发送失败时回调 */
  onError?: (message: string) => void
  /** 发送成功时回调 */
  onSuccess?: () => void
}

export interface SendVerifyCodeSession {
  mobile: string
  remaining: number
}

function getExpireStorageKey(type: VerifyCodeType, mobile: string) {
  return `${EXPIRE_STORAGE_PREFIX}:${type}:${mobile}`
}

function getMobileStorageKey(type: VerifyCodeType) {
  return `${MOBILE_STORAGE_PREFIX}:${type}`
}

function getStoredMobile(type: VerifyCodeType): string {
  return localStorage.getItem(getMobileStorageKey(type)) || ''
}

function getRemainingSeconds(type: VerifyCodeType, mobile: string): number {
  if (!MOBILE_PATTERN.test(mobile)) return 0

  const raw = localStorage.getItem(getExpireStorageKey(type, mobile))
  if (!raw) return 0

  const expireAt = Number(raw)
  if (Number.isNaN(expireAt)) {
    localStorage.removeItem(getExpireStorageKey(type, mobile))
    return 0
  }

  const remaining = Math.ceil((expireAt - Date.now()) / 1000)
  if (remaining <= 0) {
    clearSendSession(type, mobile)
    return 0
  }

  return remaining
}

/** 读取当前场景下未过期的发送会话 */
export function getActiveSendSession(type: VerifyCodeType): SendVerifyCodeSession {
  const mobile = getStoredMobile(type)
  if (!mobile) {
    return { mobile: '', remaining: 0 }
  }

  const remaining = getRemainingSeconds(type, mobile)
  if (remaining <= 0) {
    return { mobile: '', remaining: 0 }
  }

  return { mobile, remaining }
}

function saveSendSession(type: VerifyCodeType, mobile: string) {
  localStorage.setItem(
    getExpireStorageKey(type, mobile),
    String(Date.now() + COUNTDOWN_SECONDS * 1000),
  )
  localStorage.setItem(getMobileStorageKey(type), mobile)
}

function clearSendSession(type: VerifyCodeType, mobile: string) {
  localStorage.removeItem(getExpireStorageKey(type, mobile))
  if (getStoredMobile(type) === mobile) {
    localStorage.removeItem(getMobileStorageKey(type))
  }
}

/**
 * 发送短信验证码：行为验证码 → POST /user/code/send
 */
export function useSendVerifyCode(options: UseSendVerifyCodeOptions) {
  const sendingCode = ref(false)
  const codeCountdown = ref(0)
  const successMsg = ref('')

  let countdownTimer: ReturnType<typeof setInterval> | null = null

  function clearCountdownTimer() {
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }

  function updateCountdownTip() {
    if (codeCountdown.value > 0) {
      successMsg.value = `验证码已发送，${codeCountdown.value} 秒内不能重复发送`
    } else {
      successMsg.value = ''
    }
  }

  function syncCountdownFromStorage() {
    clearCountdownTimer()

    const mobile = options.getMobile() || getStoredMobile(options.type)
    const remaining = getRemainingSeconds(options.type, mobile)

    codeCountdown.value = remaining

    if (remaining <= 0) {
      successMsg.value = ''
      return
    }

    updateCountdownTip()
    countdownTimer = setInterval(() => {
      codeCountdown.value -= 1
      if (codeCountdown.value <= 0) {
        clearCountdownTimer()
        successMsg.value = ''
        clearSendSession(options.type, mobile)
      } else {
        updateCountdownTip()
      }
    }, 1000)
  }

  function restoreSessionIfNeeded() {
    const { mobile, remaining } = getActiveSendSession(options.type)
    if (remaining <= 0 || !mobile) return

    if (!MOBILE_PATTERN.test(options.getMobile())) {
      options.onRestoreSession?.(mobile)
    }

    syncCountdownFromStorage()
  }

  function startCountdown() {
    saveSendSession(options.type, options.getMobile())
    syncCountdownFromStorage()
  }

  function validateMobile(): boolean {
    if (!MOBILE_PATTERN.test(options.getMobile())) {
      options.onError?.('请输入正确的手机号')
      return false
    }
    return true
  }

  async function handleSendCode() {
    if (codeCountdown.value > 0) return

    successMsg.value = ''
    if (!validateMobile()) return

    try {
      sendingCode.value = true
      const captchaId = await showBehaviorCaptcha({
        type: options.captchaType,
      })
      await sendVerifyCode({
        mobile: options.getMobile(),
        type: options.type,
        captchaId,
      })
      startCountdown()
      options.onSuccess?.()
    } catch (err) {
      if (err instanceof CaptchaCancelledError) return
      options.onError?.(err instanceof Error ? err.message : '验证码发送失败')
    } finally {
      sendingCode.value = false
    }
  }

  onMounted(restoreSessionIfNeeded)

  watch(
    () => options.getMobile(),
    () => syncCountdownFromStorage(),
  )

  onUnmounted(clearCountdownTimer)

  return {
    sendingCode,
    codeCountdown,
    successMsg,
    handleSendCode,
  }
}
