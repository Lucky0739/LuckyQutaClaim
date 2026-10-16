import { ref } from 'vue'
import { balancePay, createSandboxPay } from '@/api/pay'
import { getPayPresentation } from '@/types/pay'
import { canPayOrder, type OrderVO } from '@/types/order'
import { openSandboxPayPage } from '@/utils/pay'

export function useOrderPay() {
  const paying = ref(false)
  const payError = ref('')

  function clearPayError() {
    payError.value = ''
  }

  async function payWithAlipay(order: OrderVO): Promise<{
    presentation: ReturnType<typeof getPayPresentation>
    payResult: Awaited<ReturnType<typeof createSandboxPay>>
  }> {
    if (!order.orderNo) {
      throw new Error('订单号无效')
    }
    if (!canPayOrder(order)) {
      throw new Error('订单已超过支付时限或状态不可支付')
    }
    paying.value = true
    clearPayError()
    try {
      const payResult = await createSandboxPay(order.orderNo)
      const presentation = getPayPresentation(payResult)
      if (presentation === 'url' || presentation === 'both') {
        openSandboxPayPage(payResult)
      }
      return { presentation, payResult }
    } catch (err) {
      payError.value = err instanceof Error ? err.message : '发起支付失败'
      throw err
    } finally {
      paying.value = false
    }
  }

  async function payWithBalance(order: OrderVO) {
    if (!order.orderNo) {
      throw new Error('订单号无效')
    }
    if (!canPayOrder(order)) {
      throw new Error('订单已超过支付时限或状态不可支付')
    }
    paying.value = true
    clearPayError()
    try {
      return await balancePay(order.orderNo)
    } catch (err) {
      payError.value = err instanceof Error ? err.message : '余额支付失败'
      throw err
    } finally {
      paying.value = false
    }
  }

  return {
    paying,
    payError,
    clearPayError,
    payWithAlipay,
    payWithBalance,
  }
}
