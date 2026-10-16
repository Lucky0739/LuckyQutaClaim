import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { getActivityGoods, resolveSeckillDisplayPage } from '@/api/activity'
import type { SeckillActivityVO, SeckillGoodsVO } from '@/types/activity'
import {
  calcRemainingSeconds,
  canPurchaseSeckillGoods,
  getActivityCountdownTarget,
  SeckillActivityStatus,
} from '@/types/activity'
import { SECKILL_DISPLAY_CHANGED } from '@/utils/seckillEvents'

const HOME_PANEL_GOODS_LIMIT = 4
const GOODS_POLL_INTERVAL_MS = 10_000

export function useHomeSeckill() {
  const loading = ref(false)
  const errorMsg = ref('')
  const activity = ref<SeckillActivityVO | null>(null)
  const goodsList = ref<SeckillGoodsVO[]>([])
  const remainingSeconds = ref(0)

  const countdownInfo = computed(() => getActivityCountdownTarget(activity.value))

  const countdownParts = computed(() => {
    const total = remainingSeconds.value
    const h = Math.floor(total / 3600)
    const m = Math.floor((total % 3600) / 60)
    const s = total % 60
    return {
      h: String(h).padStart(2, '0'),
      m: String(m).padStart(2, '0'),
      s: String(s).padStart(2, '0'),
    }
  })

  const panelGoods = computed(() => goodsList.value.slice(0, HOME_PANEL_GOODS_LIMIT))
  const hasGoods = computed(() => panelGoods.value.length > 0)
  const isEnded = computed(() => activity.value?.status === SeckillActivityStatus.ENDED)
  const seckillPurchaseEnabled = computed(() => canPurchaseSeckillGoods(activity.value))
  const activityId = computed(() => activity.value?.id ?? null)

  let timer: ReturnType<typeof setInterval> | null = null
  let goodsPollTimer: ReturnType<typeof setInterval> | null = null

  function syncCountdown() {
    remainingSeconds.value = calcRemainingSeconds(countdownInfo.value.targetTime)
  }

  function startCountdown() {
    stopCountdown()
    syncCountdown()
    timer = setInterval(syncCountdown, 1000)
  }

  function stopCountdown() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  async function refreshGoods() {
    const current = activity.value
    if (!current?.id) return
    try {
      goodsList.value = await getActivityGoods(current.id)
    } catch {
      /* 轮询刷新失败时保留当前列表 */
    }
  }

  async function loadData() {
    loading.value = true
    errorMsg.value = ''
    try {
      const resolved = await resolveSeckillDisplayPage()
      if (!resolved) {
        activity.value = null
        goodsList.value = []
        remainingSeconds.value = 0
        return
      }

      activity.value = resolved.activity
      goodsList.value = resolved.goods
      startCountdown()
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载秒杀活动失败'
      activity.value = null
      goodsList.value = []
      remainingSeconds.value = 0
    } finally {
      loading.value = false
    }
  }

  function handleSeckillDisplayChanged() {
    if (loading.value) return
    void loadData()
  }

  function stopGoodsPoll() {
    if (goodsPollTimer) {
      clearInterval(goodsPollTimer)
      goodsPollTimer = null
    }
  }

  function startGoodsPoll() {
    stopGoodsPoll()
    goodsPollTimer = setInterval(() => {
      if (document.visibilityState !== 'visible' || loading.value) return
      void refreshGoods()
    }, GOODS_POLL_INTERVAL_MS)
  }

  function handleVisibilityChange() {
    if (document.visibilityState !== 'visible') return
    void refreshGoods()
  }

  watch(countdownInfo, () => {
    syncCountdown()
  })

  onMounted(() => {
    void loadData().then(() => startGoodsPoll())
    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener(SECKILL_DISPLAY_CHANGED, handleSeckillDisplayChanged)
  })

  onUnmounted(() => {
    stopCountdown()
    stopGoodsPoll()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    window.removeEventListener(SECKILL_DISPLAY_CHANGED, handleSeckillDisplayChanged)
  })

  return {
    loading,
    errorMsg,
    activity,
    goodsList,
    panelGoods,
    hasGoods,
    isEnded,
    seckillPurchaseEnabled,
    activityId,
    countdownInfo,
    countdownParts,
    loadData,
  }
}

export function useSeckillCountdown(targetActivity: () => SeckillActivityVO | null) {
  const remainingSeconds = ref(0)

  const countdownInfo = computed(() => getActivityCountdownTarget(targetActivity()))

  const countdownParts = computed(() => {
    const total = remainingSeconds.value
    const h = Math.floor(total / 3600)
    const m = Math.floor((total % 3600) / 60)
    const s = total % 60
    return {
      h: String(h).padStart(2, '0'),
      m: String(m).padStart(2, '0'),
      s: String(s).padStart(2, '0'),
    }
  })

  let timer: ReturnType<typeof setInterval> | null = null

  function syncCountdown() {
    remainingSeconds.value = calcRemainingSeconds(countdownInfo.value.targetTime)
  }

  function startCountdown() {
    stopCountdown()
    syncCountdown()
    timer = setInterval(syncCountdown, 1000)
  }

  function stopCountdown() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  watch(countdownInfo, syncCountdown, { immediate: true })

  onUnmounted(stopCountdown)

  return {
    countdownInfo,
    countdownParts,
    remainingSeconds,
    startCountdown,
    stopCountdown,
  }
}
