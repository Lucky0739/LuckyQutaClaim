<template>
  <div class="page">
    <Header top-only />

    <main class="pay-result-main">
      <section class="pay-result-card">
        <div v-if="status === 'loading'" class="pay-result-state">
          <div class="pay-result-spinner" />
          <h1>正在确认支付结果</h1>
          <p>订单号：{{ orderNo || '--' }}</p>
          <p class="pay-result-tip">{{ resultTip }}</p>
        </div>

        <div v-else-if="status === 'success'" class="pay-result-state pay-result-state--success">
          <div class="pay-result-icon">✓</div>
          <h1>支付成功</h1>
          <p>订单号：{{ orderNo }}</p>
          <p class="pay-result-tip">库存已扣减，可在「我的订单」查看详情</p>
          <router-link to="/orders" class="pay-result-btn pay-result-btn--primary">查看我的订单</router-link>
        </div>

        <div v-else-if="status === 'pending'" class="pay-result-state pay-result-state--pending">
          <div class="pay-result-icon pay-result-icon--pending">!</div>
          <h1>暂未检测到支付成功</h1>
          <p>订单号：{{ orderNo }}</p>
          <p class="pay-result-tip">若已完成支付，请稍后再试或返回订单页重新查询</p>
          <div class="pay-result-actions">
            <button type="button" class="pay-result-btn pay-result-btn--primary" :disabled="retrying" @click="retryCheck">
              {{ retrying ? '查询中...' : '重新查询' }}
            </button>
            <router-link to="/orders" class="pay-result-btn">返回订单列表</router-link>
          </div>
        </div>

        <div v-else class="pay-result-state pay-result-state--error">
          <div class="pay-result-icon pay-result-icon--error">×</div>
          <h1>{{ errorMsg || '支付结果确认失败' }}</h1>
          <router-link to="/orders" class="pay-result-btn">返回订单列表</router-link>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import Header from '@/components/Header.vue'
import { handlePaySuccess, queryPayStatus } from '@/api/pay'
import type { PaymentMethod } from '@/types/pay'
import { pollPayStatus } from '@/utils/pay'

const route = useRoute()
const orderNo = typeof route.query.orderNo === 'string' ? route.query.orderNo.trim() : ''
const payMethod = (route.query.method === 'balance' ? 'balance' : 'alipay') as PaymentMethod
const status = ref<'loading' | 'success' | 'pending' | 'error'>('loading')
const errorMsg = ref('')
const retrying = ref(false)

const resultTip = computed(() => {
  if (status.value === 'loading') {
    return payMethod === 'balance'
      ? '请稍候，系统正在确认余额支付结果…'
      : '请稍候，系统正在与支付宝同步支付状态…'
  }
  return ''
})

async function confirmPayResult() {
  if (!orderNo) {
    status.value = 'error'
    errorMsg.value = '缺少订单号，无法确认支付结果'
    return
  }

  status.value = 'loading'
  errorMsg.value = ''
  try {
    if (payMethod === 'balance') {
      const paid = await queryPayStatus(orderNo)
      status.value = paid ? 'success' : 'pending'
      return
    }

    const paid = await pollPayStatus(orderNo)
    if (!paid) {
      status.value = 'pending'
      return
    }
    await handlePaySuccess(orderNo)
    status.value = 'success'
  } catch (err) {
    status.value = 'error'
    errorMsg.value = err instanceof Error ? err.message : '确认支付结果失败'
  }
}

async function retryCheck() {
  retrying.value = true
  try {
    await confirmPayResult()
  } finally {
    retrying.value = false
  }
}

onMounted(() => {
  confirmPayResult()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.pay-result-main {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
  padding: 48px 16px 56px;
}

.pay-result-card {
  max-width: 520px;
  margin: 0 auto;
  padding: 40px 32px;
  border-radius: 14px;
  background: #fff;
  text-align: center;
}

.pay-result-state h1 {
  margin: 0 0 12px;
  font-size: 22px;
  color: #222;
}

.pay-result-state p {
  margin: 6px 0 0;
  color: #666;
  font-size: 14px;
}

.pay-result-tip {
  margin-top: 10px !important;
  color: #999 !important;
  font-size: 13px !important;
}

.pay-result-spinner {
  width: 44px;
  height: 44px;
  margin: 0 auto 20px;
  border: 3px solid #f0f0f0;
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.pay-result-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 18px;
  border-radius: 50%;
  background: #52c41a;
  color: #fff;
  font-size: 28px;
  line-height: 56px;
}

.pay-result-icon--pending {
  background: #faad14;
}

.pay-result-icon--error {
  background: #f5222d;
}

.pay-result-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 24px;
}

.pay-result-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 120px;
  height: 38px;
  padding: 0 18px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #fff;
  color: #555;
  text-decoration: none;
  font-size: 14px;
  cursor: pointer;
}

.pay-result-btn--primary {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
}

.pay-result-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.pay-result-state--success .pay-result-btn,
.pay-result-state--error .pay-result-btn {
  margin-top: 24px;
}
</style>
