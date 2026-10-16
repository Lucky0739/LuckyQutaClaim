<template>
  <div class="auth-page">
    <div class="auth-header">
      <router-link to="/" class="auth-logo">
        <span class="logo-brand">秒杀商城</span>
      </router-link>
      <router-link to="/register" class="auth-header-link">免费注册</router-link>
    </div>

    <div class="auth-body">
      <div class="auth-card">
        <div class="auth-tabs">
          <button
            type="button"
            class="auth-tab"
            :class="{ active: loginTab === 'password' }"
            @click="loginTab = 'password'"
          >
            密码登录
          </button>
          <button
            type="button"
            class="auth-tab"
            :class="{ active: loginTab === 'code' }"
            @click="loginTab = 'code'"
          >
            验证码登录
          </button>
        </div>

        <form class="auth-form" @submit.prevent="handleLogin">
          <div class="form-item">
            <label class="form-label">手机号</label>
            <input
              v-model="form.mobile"
              type="tel"
              maxlength="11"
              autocomplete="tel"
              class="form-input"
              placeholder="请输入手机号"
            />
          </div>

          <div v-if="loginTab === 'password'" class="form-item">
            <label class="form-label">登录密码</label>
            <input
              v-model="form.password"
              type="password"
              maxlength="20"
              autocomplete="current-password"
              class="form-input"
              placeholder="请输入登录密码"
            />
          </div>

          <div v-else class="form-item">
            <label class="form-label">验证码</label>
            <div class="code-row">
              <input
                v-model="form.verifyCode"
                type="text"
                maxlength="6"
                class="form-input"
                placeholder="请输入验证码"
              />
              <button
                type="button"
                class="code-btn"
                :disabled="codeCountdown > 0 || sendingCode"
                @click="handleSendCode"
              >
                {{ sendingCode ? '发送中...' : codeCountdown > 0 ? `${codeCountdown}s 后重发` : '获取验证码' }}
              </button>
            </div>
            <p v-if="codeSuccessMsg" class="form-success">{{ codeSuccessMsg }}</p>
          </div>

          <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>

          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>

        <div class="auth-footer">
          <router-link to="/register">还没有账号？立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '@/api/user'
import { buildLoginRequest, LoginType, MOBILE_PATTERN, VerifyCodeType } from '@/types/user'
import { setToken, setUserInfo, LOGIN_MOBILE_KEY } from '@/utils/auth'
import { getActiveSendSession, useSendVerifyCode } from '@/composables/useSendVerifyCode'

const LOGIN_TAB_KEY = 'login_active_tab'

const route = useRoute()
const router = useRouter()

function getInitialLoginTab(): 'password' | 'code' {
  const queryTab = route.query.tab
  if (queryTab === 'password') return 'password'
  if (queryTab === 'code') return 'code'

  const { remaining } = getActiveSendSession(VerifyCodeType.LOGIN)
  if (remaining > 0) return 'code'
  return localStorage.getItem(LOGIN_TAB_KEY) === 'code' ? 'code' : 'password'
}

function getInitialMobile(): string {
  const queryMobile = route.query.mobile
  if (typeof queryMobile === 'string' && queryMobile) {
    return queryMobile
  }

  const remembered = localStorage.getItem(LOGIN_MOBILE_KEY)
  if (remembered) return remembered

  return getActiveSendSession(VerifyCodeType.LOGIN).mobile ?? ''
}

const loginTab = ref<'password' | 'code'>(getInitialLoginTab())
const loading = ref(false)
const errorMsg = ref('')

const form = ref({
  mobile: getInitialMobile(),
  password: '',
  verifyCode: '',
})

watch(loginTab, (tab) => {
  localStorage.setItem(LOGIN_TAB_KEY, tab)
  errorMsg.value = ''
})

const {
  sendingCode,
  codeCountdown,
  successMsg: codeSuccessMsg,
  handleSendCode,
} = useSendVerifyCode({
  getMobile: () => form.value.mobile,
  type: VerifyCodeType.LOGIN,
  onRestoreSession: (mobile) => {
    form.value.mobile = mobile
    loginTab.value = 'code'
  },
  onError: (message) => {
    errorMsg.value = message
  },
  onSuccess: () => {
    errorMsg.value = ''
    loginTab.value = 'code'
  },
})

function validateMobile() {
  if (!MOBILE_PATTERN.test(form.value.mobile)) {
    errorMsg.value = '手机号格式不正确'
    return false
  }
  return true
}

function validatePasswordLogin() {
  if (!form.value.password) {
    errorMsg.value = '请输入登录密码'
    return false
  }
  return true
}

function validateVerifyCodeLogin() {
  if (!form.value.verifyCode) {
    errorMsg.value = '请输入验证码'
    return false
  }
  return true
}

async function handleLogin() {
  errorMsg.value = ''
  if (!validateMobile()) return

  const isPasswordLogin = loginTab.value === 'password'
  if (isPasswordLogin) {
    if (!validatePasswordLogin()) return
  } else if (!validateVerifyCodeLogin()) {
    return
  }

  const type = isPasswordLogin ? LoginType.PASSWORD : LoginType.VERIFY_CODE

  try {
    loading.value = true
    const result = await login(
      buildLoginRequest({
        mobile: form.value.mobile,
        type,
        password: isPasswordLogin ? form.value.password : undefined,
        verifyCode: isPasswordLogin ? undefined : form.value.verifyCode,
      }),
    )
    localStorage.setItem(LOGIN_MOBILE_KEY, form.value.mobile)
    setToken(result.token)
    setUserInfo(result.userInfo)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect)
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@import '@/styles/auth.css';
</style>
