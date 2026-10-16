import { nextTick, onMounted, onUnmounted, ref, watch, type Ref } from 'vue'
import { getGoodsPageByShopId } from '@/api/goods'
import {
  buildShopGoodsQuery,
  HOME_GOODS_PAGE_SIZE,
  type GoodsListVO,
} from '@/types/goods'

export function useShopGoodsFeed(
  shopId: Ref<string>,
  sentinelRef: Ref<HTMLElement | null>,
) {
  const goodsList = ref<GoodsListVO[]>([])
  const pageNum = ref(1)
  const loading = ref(false)
  const finished = ref(false)
  const errorMsg = ref('')
  const total = ref(0)

  async function loadMore() {
    const id = shopId.value?.trim()
    if (!id || loading.value || finished.value) return

    loading.value = true
    errorMsg.value = ''

    const currentPage = pageNum.value

    try {
      const page = await getGoodsPageByShopId(id, buildShopGoodsQuery(currentPage))
      const records = page.records ?? []
      total.value = page.total ?? 0

      if (records.length) {
        const existingIds = new Set(goodsList.value.map((item) => item.id))
        const freshRecords = records.filter((item) => !existingIds.has(item.id))
        goodsList.value.push(...freshRecords)
      }

      const totalPages = page.totalPages ?? 0
      const isLastPage =
        records.length === 0
        || records.length < HOME_GOODS_PAGE_SIZE
        || (totalPages > 0 && currentPage >= totalPages)

      if (isLastPage) {
        finished.value = true
      } else {
        pageNum.value = currentPage + 1
      }
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载商品失败'
    } finally {
      loading.value = false
      await nextTick()
      fillViewportIfNeeded()
    }
  }

  function fillViewportIfNeeded() {
    const el = sentinelRef.value
    if (!el || loading.value || finished.value || errorMsg.value) return

    const rect = el.getBoundingClientRect()
    if (rect.top <= window.innerHeight + 120) {
      loadMore()
    }
  }

  function retry() {
    errorMsg.value = ''
    loadMore()
  }

  let observer: IntersectionObserver | null = null

  function bindObserver() {
    observer?.disconnect()
    observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          loadMore()
        }
      },
      { root: null, rootMargin: '120px 0px', threshold: 0 },
    )

    if (sentinelRef.value) {
      observer.observe(sentinelRef.value)
    }
  }

  function resetFeed() {
    goodsList.value = []
    pageNum.value = 1
    finished.value = false
    errorMsg.value = ''
    total.value = 0
  }

  async function reloadFeed() {
    resetFeed()
    await loadMore()
  }

  onMounted(async () => {
    if (!shopId.value?.trim()) return
    await loadMore()
    await nextTick()
    bindObserver()
  })

  watch(sentinelRef, (el) => {
    if (!el || !observer) return
    observer.disconnect()
    observer.observe(el)
  })

  watch(shopId, (id) => {
    if (!id?.trim()) return
    reloadFeed()
  })

  onUnmounted(() => {
    observer?.disconnect()
    observer = null
  })

  return {
    goodsList,
    loading,
    finished,
    errorMsg,
    total,
    loadMore,
    retry,
    reloadFeed,
  }
}
