import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addCart } from '@/api/cart'
import { CART_MAX_GOODS_COUNT } from '@/types/cart'
import { isLoggedIn as checkLoggedIn } from '@/utils/auth'
import { dispatchCartChanged } from '@/utils/cartEvent'

export function useAddToCart() {
  const router = useRouter()
  const route = useRoute()
  const loading = ref(false)
  const toastMsg = ref('')

  function showToast(message: string) {
    toastMsg.value = message
    window.setTimeout(() => {
      if (toastMsg.value === message) {
        toastMsg.value = ''
      }
    }, 2000)
  }

  async function addToCart(params: {
    goodsId: string
    skuId: string
    goodsCount: number
  }) {
    if (!checkLoggedIn()) {
      router.push({ path: '/login', query: { redirect: route.fullPath } })
      return false
    }
    if (!params.goodsId || !params.skuId) {
      showToast('请选择商品规格后再加入购物车')
      return false
    }
    const count = Math.min(CART_MAX_GOODS_COUNT, Math.max(1, params.goodsCount))
    if (loading.value) return false

    loading.value = true
    try {
      await addCart({
        goodsId: params.goodsId,
        skuId: params.skuId,
        goodsCount: count,
      })
      showToast('已加入购物车')
      dispatchCartChanged()
      return true
    } catch (err) {
      showToast(err instanceof Error ? err.message : '加入购物车失败')
      return false
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    toastMsg,
    addToCart,
    showToast,
  }
}
