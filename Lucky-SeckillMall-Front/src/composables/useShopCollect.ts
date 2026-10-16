import { ref } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'
import { cancelCollectShop, collectShop, getShopCollectStatus } from '@/api/shop'
import type { ShopId } from '@/types/shop'
import { isLoggedIn as checkLoggedIn } from '@/utils/auth'
import { dispatchShopCollectChanged } from '@/utils/collectEvent'

export function useShopCollect() {
  const router = useRouter()
  const route = useRoute()
  const collected = ref(false)
  const collectCount = ref(0)
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

  async function loadCollectStatus(shopId: ShopId) {
    if (!shopId) return
    try {
      const status = await getShopCollectStatus(shopId)
      collected.value = status.collected ?? status.isCollected ?? false
      collectCount.value = status.collectNum ?? status.collectCount ?? 0
    } catch {
      collected.value = false
      collectCount.value = 0
    }
  }

  function redirectToLogin(redirect?: RouteLocationRaw) {
    router.push({
      path: '/login',
      query: { redirect: typeof redirect === 'string' ? redirect : route.fullPath },
    })
  }

  async function toggleCollect(shopId: ShopId, redirect?: RouteLocationRaw) {
    if (!checkLoggedIn()) {
      redirectToLogin(redirect)
      return false
    }
    if (!shopId || loading.value) return false

    loading.value = true
    try {
      if (collected.value) {
        await cancelCollectShop({ shopId })
        showToast('已取消收藏')
      } else {
        await collectShop({ shopId })
        showToast('收藏成功')
      }
      await loadCollectStatus(shopId)
      dispatchShopCollectChanged()
      return true
    } catch (err) {
      showToast(err instanceof Error ? err.message : '操作失败，请稍后重试')
      return false
    } finally {
      loading.value = false
    }
  }

  async function cancelCollect(shopId: ShopId) {
    if (!shopId || loading.value) return false
    loading.value = true
    try {
      await cancelCollectShop({ shopId })
      showToast('已取消收藏')
      dispatchShopCollectChanged()
      return true
    } catch (err) {
      showToast(err instanceof Error ? err.message : '取消收藏失败')
      return false
    } finally {
      loading.value = false
    }
  }

  return {
    collected,
    collectCount,
    loading,
    toastMsg,
    loadCollectStatus,
    toggleCollect,
    cancelCollect,
    showToast,
  }
}
