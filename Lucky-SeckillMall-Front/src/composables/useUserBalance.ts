import { computed, ref } from 'vue'
import { getBalance, rechargeBalance } from '@/api/balance'
import type { UserBalanceVO } from '@/types/balance'
import { formatBalance } from '@/types/balance'

export const RECHARGE_PRESETS = [10, 50, 100, 200, 500, 1000] as const

export function useUserBalance() {
  const loading = ref(false)
  const recharging = ref(false)
  const errorMsg = ref('')
  const successMsg = ref('')
  const balanceInfo = ref<UserBalanceVO | null>(null)
  const rechargeAmount = ref('')
  const selectedPreset = ref<number | null>(null)

  const balanceText = computed(() => formatBalance(balanceInfo.value?.balance))

  function clearMessages() {
    errorMsg.value = ''
    successMsg.value = ''
  }

  async function loadBalance(options?: { silent?: boolean }) {
    if (!options?.silent) {
      loading.value = true
    }
    clearMessages()
    try {
      balanceInfo.value = await getBalance()
      return balanceInfo.value
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载余额失败'
      throw err
    } finally {
      if (!options?.silent) {
        loading.value = false
      }
    }
  }

  function selectPreset(amount: number) {
    selectedPreset.value = amount
    rechargeAmount.value = amount.toFixed(2)
    clearMessages()
  }

  function handleAmountInput(value: string) {
    selectedPreset.value = null
    rechargeAmount.value = value
    clearMessages()
  }

  function parseRechargeAmount(): number | null {
    const raw = rechargeAmount.value.trim()
    if (!raw) {
      errorMsg.value = '请输入充值金额'
      return null
    }

    const amount = Number(raw)
    if (!Number.isFinite(amount) || amount < 0.01) {
      errorMsg.value = '充值金额必须大于 0'
      return null
    }

    return Math.round(amount * 100) / 100
  }

  async function submitRecharge() {
    const amount = parseRechargeAmount()
    if (amount == null) return false

    clearMessages()
    recharging.value = true
    try {
      balanceInfo.value = await rechargeBalance({ amount })
      successMsg.value = `充值成功，当前余额 ¥${formatBalance(balanceInfo.value.balance)}`
      rechargeAmount.value = ''
      selectedPreset.value = null
      return true
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '充值失败'
      return false
    } finally {
      recharging.value = false
    }
  }

  return {
    loading,
    recharging,
    errorMsg,
    successMsg,
    balanceInfo,
    rechargeAmount,
    selectedPreset,
    balanceText,
    loadBalance,
    selectPreset,
    handleAmountInput,
    submitRecharge,
    clearMessages,
  }
}
