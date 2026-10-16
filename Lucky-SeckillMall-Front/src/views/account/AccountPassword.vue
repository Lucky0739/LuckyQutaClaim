<template>
  <section>
    <div class="account-tabs">
      <span class="account-tab active">修改密码</span>
    </div>

    <div class="account-panel">
      <div class="account-tip-box">
        修改密码需要校验原密码和绑定手机验证码。修改成功后需重新登录。
      </div>

      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>
      <p v-if="successMsg" class="account-msg account-msg--success">{{ successMsg }}</p>

      <form class="account-form" @submit.prevent="submit">
        <div class="account-form-row">
          <label class="account-form-label">绑定手机</label>
          <div class="account-form-field">
            <input :value="maskedMobile" type="text" class="account-input" readonly />
            <p v-if="!mobile" class="account-tip account-tip--warn">
              未找到登录手机号，请退出后使用手机号重新登录
            </p>
          </div>
        </div>

        <div class="account-form-row">
          <label class="account-form-label">原密码</label>
          <div class="account-form-field">
            <input
              v-model="oldPassword"
              type="password"
              maxlength="20"
              autocomplete="current-password"
              class="account-input"
              placeholder="请输入当前登录密码"
            />
          </div>
        </div>

        <div class="account-form-row">
          <label class="account-form-label">新密码</label>
          <div class="account-form-field">
            <input
              v-model="newPassword"
              type="password"
              maxlength="20"
              autocomplete="new-password"
              class="account-input"
              placeholder="请输入 6-20 位新密码"
            />
          </div>
        </div>

        <div class="account-form-row">
          <label class="account-form-label">确认新密码</label>
          <div class="account-form-field">
            <input
              v-model="confirmPassword"
              type="password"
              maxlength="20"
              autocomplete="new-password"
              class="account-input"
              placeholder="请再次输入新密码"
            />
          </div>
        </div>

        <div class="account-form-row">
          <label class="account-form-label">短信验证码</label>
          <div class="account-form-field">
            <div class="password-code-row">
              <input
                v-model="verifyCode"
                type="text"
                inputmode="numeric"
                maxlength="6"
                class="account-input"
                placeholder="请输入验证码"
              />
              <button
                type="button"
                class="account-btn password-code-btn"
                :disabled="!mobile || sendingCode || codeCountdown > 0"
                @click="handleSendCode"
              >
                {{
                  sendingCode
                    ? '发送中...'
                    : codeCountdown > 0
                      ? `${codeCountdown}s 后重发`
                      : '获取验证码'
                }}
              </button>
            </div>
            <p v-if="codeSuccessMsg" class="account-msg account-msg--success">
              {{ codeSuccessMsg }}
            </p>
          </div>
        </div>

        <div class="password-actions">
          <button
            type="submit"
            class="account-btn account-btn-primary"
            :disabled="submitting || !mobile"
          >
            {{ submitting ? '提交中...' : '确认修改' }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '@/api/user'
import { useSendVerifyCode } from '@/composables/useSendVerifyCode'
import { VerifyCodeType } from '@/types/user'
import { clearAuth, getStoredLoginMobile } from '@/utils/auth'

const router = useRouter()
const mobile = getStoredLoginMobile().trim()
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const verifyCode = ref('')
const submitting = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

const maskedMobile = computed(() => {
  if (!mobile) return '未获取到手机号'
  return `${mobile.slice(0, 3)}****${mobile.slice(-4)}`
})

const {
  sendingCode,
  codeCountdown,
  successMsg: codeSuccessMsg,
  handleSendCode,
} = useSendVerifyCode({
  getMobile: () => mobile,
  type: VerifyCodeType.CHANGE_PASSWORD,
  onError: (message) => {
    errorMsg.value = message
  },
  onSuccess: () => {
    errorMsg.value = ''
  },
})

function validate(): string | null {
  if (!oldPassword.value) return '请输入原密码'
  if (newPassword.value.length < 6 || newPassword.value.length > 20) {
    return '新密码长度需为 6-20 位'
  }
  if (newPassword.value === oldPassword.value) return '新密码不能与原密码相同'
  if (confirmPassword.value !== newPassword.value) return '两次输入的新密码不一致'
  if (!/^\d{4,6}$/.test(verifyCode.value.trim())) return '请输入正确的短信验证码'
  return null
}

async function submit() {
  if (submitting.value) return
  errorMsg.value = ''
  successMsg.value = ''

  const validationError = validate()
  if (validationError) {
    errorMsg.value = validationError
    return
  }

  submitting.value = true
  try {
    await changePassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
      verifyCode: verifyCode.value.trim(),
    })
    successMsg.value = '密码修改成功，即将跳转到登录页'
    window.setTimeout(() => {
      clearAuth()
      void router.replace('/login')
    }, 1000)
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '修改密码失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.account-tip--warn {
  color: #cf1322;
}

.password-code-row {
  display: flex;
  gap: 10px;
}

.password-code-row .account-input {
  flex: 1;
  min-width: 0;
}

.password-code-btn {
  flex-shrink: 0;
  min-width: 112px;
  white-space: nowrap;
}

.password-actions {
  display: flex;
  gap: 12px;
}

.password-actions .account-btn {
  min-width: 120px;
}
</style>
