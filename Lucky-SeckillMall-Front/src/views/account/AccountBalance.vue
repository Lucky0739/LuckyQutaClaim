<template>
  <section>
    <div class="account-tabs">
      <button
        type="button"
        class="account-tab"
        :class="{ active: activeTab === 'overview' }"
        @click="switchTab('overview')"
      >
        余额概览
      </button>
      <button
        type="button"
        class="account-tab"
        :class="{ active: activeTab === 'recharge' }"
        @click="switchTab('recharge')"
      >
        余额充值
      </button>
    </div>

    <div class="account-panel balance-panel">
      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>
      <p v-if="successMsg" class="account-msg account-msg--success">{{ successMsg }}</p>

      <div v-if="loading" class="address-loading">
        <span class="address-loading-dot"></span>
        加载中...
      </div>

      <template v-else-if="activeTab === 'overview'">
        <div class="balance-card">
          <p class="balance-card-label">当前余额（元）</p>
          <p class="balance-card-amount">¥ {{ balanceText }}</p>
          <p class="balance-card-tip">余额可用于商城内余额支付，充值后立即到账</p>
        </div>

        <div class="balance-overview-actions">
          <button
            type="button"
            class="account-btn account-btn-primary"
            :disabled="refreshing"
            @click="handleRefresh"
          >
            {{ refreshing ? '刷新中...' : '刷新余额' }}
          </button>
          <button type="button" class="account-btn" @click="switchTab('recharge')">
            去充值
          </button>
        </div>
      </template>

      <template v-else>
        <div class="balance-card balance-card--compact">
          <p class="balance-card-label">当前余额（元）</p>
          <p class="balance-card-amount balance-card-amount--compact">¥ {{ balanceText }}</p>
        </div>

        <div class="balance-recharge">
          <h3 class="balance-recharge-title">余额充值</h3>

          <div class="balance-presets">
            <button
              v-for="amount in RECHARGE_PRESETS"
              :key="amount"
              type="button"
              class="balance-preset-btn"
              :class="{ 'balance-preset-btn--active': selectedPreset === amount }"
              @click="selectPreset(amount)"
            >
              ¥{{ amount }}
            </button>
          </div>

          <form class="account-form balance-form" @submit.prevent="handleRecharge">
            <div class="account-form-row">
              <label class="account-form-label">充值金额</label>
              <div class="account-form-field">
                <div class="balance-input-wrap">
                  <span class="balance-input-prefix">¥</span>
                  <input
                    :value="rechargeAmount"
                    type="text"
                    inputmode="decimal"
                    maxlength="12"
                    class="account-input balance-input"
                    placeholder="请输入充值金额，最低 0.01 元"
                    @input="handleAmountInput(($event.target as HTMLInputElement).value)"
                  />
                </div>
                <p class="account-tip">支持自定义金额，单次充值最低 0.01 元</p>
              </div>
            </div>

            <div class="account-actions">
              <button
                type="submit"
                class="account-btn account-btn-primary"
                :disabled="recharging"
              >
                {{ recharging ? '充值中...' : '立即充值' }}
              </button>
            </div>
          </form>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RECHARGE_PRESETS, useUserBalance } from '@/composables/useUserBalance'

type TabType = 'overview' | 'recharge'

const activeTab = ref<TabType>('overview')
const refreshing = ref(false)

const {
  loading,
  recharging,
  errorMsg,
  successMsg,
  rechargeAmount,
  selectedPreset,
  balanceText,
  loadBalance,
  selectPreset,
  handleAmountInput,
  submitRecharge,
  clearMessages,
} = useUserBalance()

function switchTab(tab: TabType) {
  clearMessages()
  activeTab.value = tab
}

async function handleRefresh() {
  refreshing.value = true
  clearMessages()
  try {
    await loadBalance({ silent: true })
    successMsg.value = '余额已刷新'
  } catch {
    // errorMsg 已在 loadBalance 中设置
  } finally {
    refreshing.value = false
  }
}

async function handleRecharge() {
  await submitRecharge()
}

onMounted(loadBalance)
</script>

<style scoped>
.balance-panel {
  max-width: 640px;
}

.balance-card {
  padding: 24px 28px;
  margin-bottom: 28px;
  background: linear-gradient(135deg, #fff7e6 0%, #fff 100%);
  border: 1px solid #ffd591;
}

.balance-card--compact {
  margin-bottom: 20px;
  padding: 18px 24px;
}

.balance-card-label {
  margin: 0 0 8px;
  font-size: 13px;
  color: #666;
}

.balance-card-amount {
  margin: 0 0 10px;
  font-size: 36px;
  font-weight: 700;
  color: var(--accent);
  line-height: 1.2;
}

.balance-card-amount--compact {
  margin: 0;
  font-size: 28px;
}

.balance-card-tip {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.balance-overview-actions {
  display: flex;
  gap: 12px;
}

.balance-recharge-title {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.balance-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.balance-preset-btn {
  min-width: 88px;
  height: 36px;
  padding: 0 14px;
  border: 1px solid #ddd;
  background: #fff;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}

.balance-preset-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.balance-preset-btn--active {
  border-color: var(--accent);
  color: var(--accent);
  background: #fff5eb;
  font-weight: 600;
}

.balance-form {
  margin-top: 4px;
}

.balance-input-wrap {
  display: flex;
  align-items: center;
  max-width: 280px;
  border: 1px solid #ccc;
  background: #fff;
  transition: border-color 0.2s;
}

.balance-input-wrap:focus-within {
  border-color: var(--accent);
}

.balance-input-prefix {
  padding: 0 10px;
  font-size: 13px;
  color: #666;
  line-height: 32px;
  border-right: 1px solid #eee;
}

.balance-input {
  max-width: none;
  border: none;
}

.balance-input:focus {
  border-color: transparent;
}
</style>
