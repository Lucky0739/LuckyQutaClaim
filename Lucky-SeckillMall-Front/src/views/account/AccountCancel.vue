<template>
  <section>
    <div class="account-tabs">
      <span class="account-tab active">注销账号</span>
    </div>

    <div class="account-panel">
      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>

      <div v-if="loading" class="address-loading">
        <span class="address-loading-dot"></span>
        加载中...
      </div>

      <div v-else-if="isAdminUser" class="cancel-blocked">
        <span class="cancel-blocked-icon">⚠️</span>
        <h2 class="cancel-blocked-title">管理员账号无法自助注销</h2>
        <p class="cancel-blocked-desc">
          为保障平台安全，管理员账号不支持在此自行注销。如需停用账号，请联系超级管理员处理。
        </p>
        <router-link to="/account/profile" class="account-btn account-btn-primary cancel-blocked-btn">
          返回个人资料
        </router-link>
      </div>

      <div v-else class="cancel-content">
        <div class="account-tip-box account-tip-box--warn">
          <i class="account-tip-icon"></i>
          <span>
            注销后账号将被永久删除，订单、收藏等数据无法恢复。请确认后再操作。
          </span>
        </div>

        <form class="account-form" @submit.prevent="submitCancel">
          <div class="account-form-row">
            <label class="account-form-label">绑定手机</label>
            <div class="account-form-field">
              <input :value="maskedMobile" type="text" class="account-input" readonly />
              <p v-if="!canSendCode" class="account-tip account-tip--warn">
                未找到登录手机号记录，请退出后重新登录再申请注销
              </p>
              <p v-else class="account-tip">验证码将发送至您的绑定手机</p>
            </div>
          </div>

          <div class="account-form-row">
            <label class="account-form-label">登录密码</label>
            <div class="account-form-field">
              <input
                v-model="password"
                type="password"
                maxlength="20"
                autocomplete="current-password"
                class="account-input"
                placeholder="请输入登录密码确认身份"
              />
            </div>
          </div>

          <div class="account-form-row">
            <label class="account-form-label">短信验证码</label>
            <div class="account-form-field">
              <div class="cancel-code-row">
                <input
                  v-model="verifyCode"
                  type="text"
                  maxlength="6"
                  class="account-input"
                  placeholder="请输入验证码"
                />
                <button
                  type="button"
                  class="account-btn cancel-code-btn"
                  :disabled="!canSendCode || codeCountdown > 0 || sendingCode"
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
              <p v-if="codeSuccessMsg" class="account-msg account-msg--success">{{ codeSuccessMsg }}</p>
            </div>
          </div>

          <div class="account-actions">
            <button
              type="submit"
              class="account-btn account-btn-danger"
              :disabled="submitting || !canSendCode"
            >
              {{ submitting ? '注销中...' : '确认注销账号' }}
            </button>
            <router-link to="/account/profile" class="account-btn">取消</router-link>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useCancelAccount } from '@/composables/useCancelAccount'

const {
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
} = useCancelAccount()
</script>

<style scoped>
.cancel-blocked {
  padding: 48px 24px;
  text-align: center;
}

.cancel-blocked-icon {
  display: block;
  margin-bottom: 12px;
  font-size: 36px;
}

.cancel-blocked-title {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.cancel-blocked-desc {
  margin: 0 0 24px;
  font-size: 14px;
  line-height: 1.7;
  color: #999;
  max-width: 420px;
  margin-left: auto;
  margin-right: auto;
}

.cancel-blocked-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 140px;
  text-decoration: none;
}

.account-tip-box--warn {
  background: #fff7e6;
  border-color: #ffd591;
  color: #ad6800;
}

.account-tip--warn {
  color: #cf1322;
}

.cancel-code-row {
  display: flex;
  gap: 10px;
}

.cancel-code-row .account-input {
  flex: 1;
  min-width: 0;
}

.cancel-code-btn {
  flex-shrink: 0;
  min-width: 112px;
  white-space: nowrap;
}

.account-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.account-actions .account-btn {
  min-width: 120px;
}
</style>
