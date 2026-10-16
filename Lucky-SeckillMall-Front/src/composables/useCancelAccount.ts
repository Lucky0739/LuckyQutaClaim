import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { cancelAccount, getUserProfile } from '@/api/user'
import { useSendVerifyCode } from '@/composables/useSendVerifyCode'
import { MOBILE_PATTERN, VerifyCodeType } from '@/types/user'
import { clearAuth, getStoredLoginMobile, isAdmin } from '@/utils/auth'

export function useCancelAccount() {
  const router = useRouter()

  const loading = ref(false)
  const submitting = ref(false)
  const errorMsg = ref('')
  const maskedMobile = ref('')
  const password = ref('')
  const verifyCode = ref('')

  const isAdminUser = computed(() => isAdmin())
  const loginMobile = computed(() => getStoredLoginMobile())
  const canSendCode = computed(() => MOBILE_PATTERN.test(loginMobile.value))

  const {
    sendingCode,
    codeCountdown,
    successMsg: codeSuccessMsg,
    handleSendCode,
  } = useSendVerifyCode({
    getMobile: () => loginMobile.value,
    type: VerifyCodeType.CANCEL_ACCOUNT,
    onError: (message) => {
      errorMsg.value = message
    },
  })

  async function loadPageData() {
    loading.value = true
    errorMsg.value = ''
    try {
      const profile = await getUserProfile()
      maskedMobile.value = profile.mobile
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载账号信息失败'
    } finally {
      loading.value = false
    }
  }

  async function submitCancel() {
    if (isAdminUser.value || submitting.value) return

    const pwd = password.value
    const code = verifyCode.value.trim()

    if (!pwd) {
      errorMsg.value = '请输入登录密码'
      return
    }
    if (!code) {
      errorMsg.value = '请输入短信验证码'
      return
    }
    if (!canSendCode.value) {
      errorMsg.value = '无法获取绑定手机号，请重新登录后再试'
      return
    }

    submitting.value = true
    errorMsg.value = ''
    try {
      await cancelAccount({ password: pwd, verifyCode: code })
      clearAuth()
      await router.replace('/login')
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '注销账号失败'
    } finally {
      submitting.value = false
    }
  }

  onMounted(loadPageData)

  return {
    loading,
    submitting,
    errorMsg,
    maskedMobile,
    password,
    verifyCode,
    isAdminUser,
    canSendCode,
    sendingCode,
    codeCountdown,
    codeSuccessMsg,
    handleSendCode,
    submitCancel,
  }
}
