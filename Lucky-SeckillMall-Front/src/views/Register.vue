<template>
  <div class="auth-page">
    <div class="auth-header">
      <router-link to="/" class="auth-logo">
        <span class="logo-brand">秒杀商城</span>
      </router-link>
      <router-link to="/login" class="auth-header-link">已有账号，请登录</router-link>
    </div>

    <div class="auth-body">
      <div class="auth-card">
        <h2 class="auth-title">欢迎注册</h2>
        <p class="auth-subtitle">注册秒杀商城账号，畅享限时秒杀</p>

        <form class="auth-form" @submit.prevent="handleRegister">
          <div class="form-item">
            <label class="form-label">手机号</label>
            <input
              v-model="form.mobile"
              type="tel"
              maxlength="11"
              class="form-input"
              placeholder="请输入手机号"
            />
          </div>

          <div class="form-item">
            <label class="form-label">验证码</label>
            <div class="code-row">
              <input
                v-model="form.verifyCode"
                type="text"
                maxlength="6"
                class="form-input"
                placeholder="请输入短信验证码"
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

          <div class="form-item">
            <label class="form-label">设置密码</label>
            <input
              v-model="form.password"
              type="password"
              class="form-input"
              placeholder="请设置 6-20 位登录密码"
            />
          </div>

          <div class="form-item">
            <label class="form-label">确认密码</label>
            <input
              v-model="form.confirmPassword"
              type="password"
              class="form-input"
              placeholder="请再次输入密码"
            />
          </div>

          <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>

          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '注册中...' : '同意协议并注册' }}
          </button>
        </form>

        <div class="auth-footer">
          <router-link to="/login">已有账号？直接登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/user'
import { VerifyCodeType } from '@/types/user'
import { getActiveSendSession, useSendVerifyCode } from '@/composables/useSendVerifyCode'

const router = useRouter()

const loading = ref(false)
const errorMsg = ref('')

const form = ref({
  mobile: '',
  verifyCode: '',
  password: '',
  confirmPassword: '',
})

const activeSession = getActiveSendSession(VerifyCodeType.REGISTER)
if (activeSession.mobile) {
  form.value.mobile = activeSession.mobile
}

const mobilePattern = /^1[3-9]\d{9}$/

const {
  sendingCode,
  codeCountdown,
  successMsg: codeSuccessMsg,
  handleSendCode,
} = useSendVerifyCode({
  getMobile: () => form.value.mobile,
  type: VerifyCodeType.REGISTER,
  onRestoreSession: (mobile) => {
    form.value.mobile = mobile
  },
  onError: (message) => {
    errorMsg.value = message
  },
  onSuccess: () => {
    errorMsg.value = ''
  },
})

function validateMobile() {
  if (!mobilePattern.test(form.value.mobile)) {
    errorMsg.value = '请输入正确的手机号'
    return false
  }
  return true
}

async function handleRegister() {
  errorMsg.value = ''
  if (!validateMobile()) return

  if (!form.value.verifyCode) {
    errorMsg.value = '请输入验证码'
    return
  }
  if (!form.value.password || form.value.password.length < 6) {
    errorMsg.value = '密码长度不能少于 6 位'
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  try {
    loading.value = true
    await register({
      mobile: form.value.mobile,
      password: form.value.password,
      verifyCode: form.value.verifyCode,
    })
    router.push({
      path: '/login',
      query: { mobile: form.value.mobile, tab: 'password' },
    })
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@import '@/styles/auth.css';
</style>
