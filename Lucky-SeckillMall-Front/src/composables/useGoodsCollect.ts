import { ref } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'
import { cancelCollectGoods, collectGoods, getGoodsCollectStatus } from '@/api/goods'
import type { GoodsId } from '@/types/goods'
import { isLoggedIn as checkLoggedIn } from '@/utils/auth'
import { dispatchGoodsCollectChanged } from '@/utils/collectEvent'

export function useGoodsCollect() {
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

  async function loadCollectStatus(goodsId: GoodsId) {
    if (!goodsId) return
    try {
      const status = await getGoodsCollectStatus(goodsId)
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

  async function toggleCollect(goodsId: GoodsId, redirect?: RouteLocationRaw) {
    if (!checkLoggedIn()) {
      redirectToLogin(redirect)
      return false
    }
    if (!goodsId || loading.value) return false

    loading.value = true
    try {
      if (collected.value) {
        await cancelCollectGoods({ goodsId })
        showToast('已取消收藏')
      } else {
        await collectGoods({ goodsId })
        showToast('收藏成功')
      }
      await loadCollectStatus(goodsId)
      dispatchGoodsCollectChanged()
      return true
    } catch (err) {
      showToast(err instanceof Error ? err.message : '操作失败，请稍后重试')
      return false
    } finally {
      loading.value = false
    }
  }

  async function cancelCollect(goodsId: GoodsId) {
    if (!goodsId || loading.value) return false
    loading.value = true
    try {
      await cancelCollectGoods({ goodsId })
      showToast('已取消收藏')
      dispatchGoodsCollectChanged()
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

export function formatCollectTime(value?: string): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
