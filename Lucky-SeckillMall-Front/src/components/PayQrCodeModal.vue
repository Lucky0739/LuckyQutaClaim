<template>
  <Teleport to="body">
    <div v-if="visible" class="pay-qr-mask" @click.self="handleClose">
      <div class="pay-qr-modal">
        <div class="pay-qr-header">
          <h2>扫码支付</h2>
          <button type="button" :disabled="confirming" @click="handleClose">×</button>
        </div>

        <div class="pay-qr-body">
          <p class="pay-qr-order">订单号：{{ orderNo }}</p>
          <p v-if="amount != null" class="pay-qr-amount">应付金额：<strong>¥{{ formatOrderAmount(amount) }}</strong></p>
          <p
            class="pay-qr-countdown"
            :class="{ 'pay-qr-countdown--expired': payExpired }"
          >
            {{ countdownText }}
          </p>

          <div v-if="payExpired" class="pay-qr-expired-tip">
            订单已超过 15 分钟未支付，已自动关闭
          </div>

          <template v-else>
            <div class="pay-qr-image-wrap">
              <div v-if="qrLoading" class="pay-qr-loading">二维码加载中...</div>
              <img v-else-if="qrImageUrl" :src="qrImageUrl" alt="支付二维码" class="pay-qr-image" />
              <div v-else class="pay-qr-loading pay-qr-loading--error">{{ qrError || '二维码加载失败' }}</div>
            </div>

            <p class="pay-qr-tip">请使用支付宝沙箱 App 扫描二维码完成支付</p>

            <p v-if="status === 'polling'" class="pay-qr-status pay-qr-status--polling">
              <span class="pay-qr-dot" />正在等待支付结果…
            </p>
            <p v-else-if="status === 'success'" class="pay-qr-status pay-qr-status--success">支付成功</p>
            <p v-else-if="status === 'timeout'" class="pay-qr-status pay-qr-status--timeout">
              暂未检测到支付，请确认是否已完成扫码
            </p>
          </template>

          <p v-if="actionError" class="pay-qr-error">{{ actionError }}</p>
        </div>

        <div class="pay-qr-footer">
          <template v-if="!payExpired">
            <button
              v-if="payUrl"
              type="button"
              class="pay-qr-btn"
              :disabled="confirming"
              @click="openWebPay"
            >
              网页支付
            </button>
            <button
              type="button"
              class="pay-qr-btn pay-qr-btn--primary"
              :disabled="confirming || qrLoading"
              @click="checkPayStatus"
            >
              {{ confirming ? '确认中...' : '我已完成支付' }}
            </button>
          </template>
          <button v-else type="button" class="pay-qr-btn pay-qr-btn--primary" @click="handleClose">
            知道了
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { handlePaySuccess } from '@/api/pay'
import { closeOrderIfExpired } from '@/composables/useOrderExpire'
import { ORDER_PAY_TIMEOUT_MINUTES } from '@/constants/order'
import { formatOrderAmount } from '@/types/order'
import {
  formatPayCountdown,
  getOrderPayRemainMs,
  isOrderPayExpired,
} from '@/utils/orderExpire'
import { pollPayStatus, openSandboxPayPage } from '@/utils/pay'
import { renderQrCodeDataUrl } from '@/utils/qrcode'

const props = defineProps<{
  visible: boolean
  orderNo: string
  qrCode: string
  payUrl?: string
  amount?: number
  expireTime?: string
  createTime?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: [orderNo: string]
  expired: [orderNo: string]
}>()

const qrImageUrl = ref('')
const qrLoading = ref(false)
const qrError = ref('')
const status = ref<'idle' | 'polling' | 'success' | 'timeout'>('idle')
const confirming = ref(false)
const actionError = ref('')
const nowTick = ref(0)
const payExpired = ref(false)
let pollTimer = 0
let pollStopped = false
let countdownTimer = 0
let expiredHandled = false

const countdownText = computed(() => {
  void nowTick.value
  if (payExpired.value) return '支付已超时'
  const remain = getOrderPayRemainMs({
    status: 0,
    expireTime: props.expireTime,
    createTime: props.createTime,
  })
  return `请在 ${formatPayCountdown(remain)} 内完成支付（${ORDER_PAY_TIMEOUT_MINUTES} 分钟超时）`
})

function handleClose() {
  if (confirming.value) return
  stopPolling()
  stopCountdown()
  emit('update:visible', false)
}

function openWebPay() {
  const payUrl = props.payUrl?.trim()
  if (!payUrl) return
  try {
    openSandboxPayPage({ payUrl })
  } catch (err) {
    actionError.value = err instanceof Error ? err.message : '打开网页支付失败'
  }
}

function stopPolling() {
  pollStopped = true
  if (pollTimer) {
    window.clearTimeout(pollTimer)
    pollTimer = 0
  }
}

function stopCountdown() {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
    countdownTimer = 0
  }
}

function checkPayTimeout() {
  const expired = isOrderPayExpired({
    status: 0,
    expireTime: props.expireTime,
    createTime: props.createTime,
  })
  if (!expired) return false

  payExpired.value = true
  stopPolling()
  if (!expiredHandled && props.orderNo) {
    expiredHandled = true
    void closeOrderIfExpired({
      orderNo: props.orderNo,
      status: 0,
      expireTime: props.expireTime,
      createTime: props.createTime,
    }).then(() => {
      emit('expired', props.orderNo)
    })
  }
  return true
}

function startCountdown() {
  stopCountdown()
  countdownTimer = window.setInterval(() => {
    nowTick.value += 1
    checkPayTimeout()
  }, 1000)
}

async function loadQrImage() {
  qrLoading.value = true
  qrError.value = ''
  qrImageUrl.value = ''
  try {
    qrImageUrl.value = await renderQrCodeDataUrl(props.qrCode)
  } catch (err) {
    qrError.value = err instanceof Error ? err.message : '二维码生成失败'
  } finally {
    qrLoading.value = false
  }
}

async function finishPayFlow() {
  await handlePaySuccess(props.orderNo)
  status.value = 'success'
  emit('success', props.orderNo)
  stopPolling()
  stopCountdown()
  window.setTimeout(() => {
    emit('update:visible', false)
  }, 800)
}

async function checkPayStatus() {
  if (!props.orderNo || confirming.value || payExpired.value) return
  if (checkPayTimeout()) return

  confirming.value = true
  actionError.value = ''
  status.value = 'polling'
  try {
    const paid = await pollPayStatus(props.orderNo, { intervalMs: 1500, maxAttempts: 5 })
    if (paid) {
      await finishPayFlow()
      return
    }
    status.value = 'timeout'
    startBackgroundPolling()
  } catch (err) {
    actionError.value = err instanceof Error ? err.message : '确认支付失败'
    status.value = 'timeout'
  } finally {
    confirming.value = false
  }
}

async function startBackgroundPolling() {
  stopPolling()
  pollStopped = false
  if (status.value !== 'success') {
    status.value = 'polling'
  }

  const pollOnce = async (attempt: number) => {
    if (pollStopped || !props.visible || status.value === 'success' || payExpired.value) return
    if (checkPayTimeout()) return

    try {
      const { queryPayStatus } = await import('@/api/pay')
      const paid = await queryPayStatus(props.orderNo)
      if (paid) {
        await finishPayFlow()
        return
      }
    } catch {
      // 后台轮询忽略瞬时错误
    }

    if (attempt >= 40) {
      status.value = 'timeout'
      return
    }

    pollTimer = window.setTimeout(() => {
      pollOnce(attempt + 1)
    }, 2000)
  }

  pollOnce(0)
}

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      stopPolling()
      stopCountdown()
      status.value = 'idle'
      actionError.value = ''
      payExpired.value = false
      expiredHandled = false
      return
    }

    status.value = 'idle'
    actionError.value = ''
    payExpired.value = false
    expiredHandled = false
    startCountdown()
    if (checkPayTimeout()) return
    loadQrImage()
    startBackgroundPolling()
  },
)

onUnmounted(() => {
  stopPolling()
  stopCountdown()
})
</script>

<style scoped>
.pay-qr-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.45);
}

.pay-qr-modal {
  width: min(420px, 100%);
  border-radius: 14px;
  background: #fff;
  overflow: hidden;
}

.pay-qr-header,
.pay-qr-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.pay-qr-footer {
  justify-content: flex-end;
  border-top: 1px solid #f0f0f0;
  border-bottom: none;
}

.pay-qr-header h2 {
  margin: 0;
  font-size: 18px;
}

.pay-qr-header button {
  border: none;
  background: none;
  color: #999;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
}

.pay-qr-body {
  padding: 20px;
  text-align: center;
}

.pay-qr-order {
  margin: 0;
  color: #666;
  font-size: 13px;
}

.pay-qr-amount {
  margin: 8px 0 0;
  color: #333;
  font-size: 14px;
}

.pay-qr-amount strong {
  color: var(--accent);
  font-size: 22px;
}

.pay-qr-countdown {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--accent);
}

.pay-qr-countdown--expired {
  color: #f5222d;
}

.pay-qr-expired-tip {
  margin: 16px 0;
  padding: 12px;
  border-radius: 8px;
  background: #fff1f0;
  color: #f5222d;
  font-size: 13px;
}

.pay-qr-image-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 240px;
  height: 240px;
  margin: 18px auto 12px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  background: #fafafa;
}

.pay-qr-image {
  width: 220px;
  height: 220px;
  object-fit: contain;
}

.pay-qr-loading {
  color: #999;
  font-size: 13px;
}

.pay-qr-loading--error {
  padding: 0 16px;
  color: #f5222d;
}

.pay-qr-tip {
  margin: 0;
  color: #999;
  font-size: 13px;
}

.pay-qr-status {
  margin: 12px 0 0;
  font-size: 13px;
}

.pay-qr-status--polling {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

.pay-qr-status--success {
  color: #52c41a;
}

.pay-qr-status--timeout {
  color: #faad14;
}

.pay-qr-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.9);
  }

  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.pay-qr-error {
  margin: 10px 0 0;
  color: #f5222d;
  font-size: 13px;
}

.pay-qr-btn {
  height: 36px;
  padding: 0 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #fff;
  color: #555;
  cursor: pointer;
}

.pay-qr-btn--primary {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
}

.pay-qr-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
