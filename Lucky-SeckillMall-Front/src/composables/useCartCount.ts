import { onMounted, onUnmounted, ref } from 'vue'
import { getCartCount } from '@/api/cart'
import { AUTH_CHANGED_EVENT, isLoggedIn as checkLoggedIn } from '@/utils/auth'
import { CART_CHANGED_EVENT } from '@/utils/cartEvent'

export function useCartCount() {
  const cartCount = ref(0)
  const loading = ref(false)

  async function refreshCartCount() {
    if (!checkLoggedIn()) {
      cartCount.value = 0
      return
    }
    loading.value = true
    try {
      const count = await getCartCount()
      cartCount.value = checkLoggedIn() ? count : 0
    } catch {
      cartCount.value = 0
    } finally {
      loading.value = false
    }
  }

  function formatCartBadge(count: number): string | undefined {
    if (count <= 0) return undefined
    return count > 99 ? '99+' : String(count)
  }

  onMounted(() => {
    void refreshCartCount()
    window.addEventListener(AUTH_CHANGED_EVENT, refreshCartCount)
    window.addEventListener(CART_CHANGED_EVENT, refreshCartCount)
  })

  onUnmounted(() => {
    window.removeEventListener(AUTH_CHANGED_EVENT, refreshCartCount)
    window.removeEventListener(CART_CHANGED_EVENT, refreshCartCount)
  })

  return {
    cartCount,
    loading,
    refreshCartCount,
    formatCartBadge,
  }
}
