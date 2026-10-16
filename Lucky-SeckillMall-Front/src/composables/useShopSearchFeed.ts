import { nextTick, onMounted, onUnmounted, ref, watch, type Ref } from 'vue'
import { getPublicShopList } from '@/api/shop'
import {
  buildShopSearchQuery,
  HOME_SHOP_PAGE_SIZE,
  type ShopListVO,
} from '@/types/shop'

export function useShopSearchFeed(
  sentinelRef: Ref<HTMLElement | null>,
  shopName?: Ref<string | undefined>,
) {
  const shopList = ref<ShopListVO[]>([])
  const pageNum = ref(1)
  const loading = ref(false)
  const finished = ref(false)
  const errorMsg = ref('')

  async function loadMore() {
    if (loading.value || finished.value) return

    loading.value = true
    errorMsg.value = ''

    const currentPage = pageNum.value

    try {
      const keyword = shopName?.value?.trim()
      const page = await getPublicShopList(buildShopSearchQuery(currentPage, keyword))
      const records = page.records ?? []

      if (records.length) {
        const existingIds = new Set(shopList.value.map((item) => item.id))
        const freshRecords = records.filter((item) => !existingIds.has(item.id))
        shopList.value.push(...freshRecords)
      }

      const totalPages = page.totalPages ?? 0
      const isLastPage =
        records.length === 0
        || records.length < HOME_SHOP_PAGE_SIZE
        || (totalPages > 0 && currentPage >= totalPages)

      if (isLastPage) {
        finished.value = true
      } else {
        pageNum.value = currentPage + 1
      }
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载店铺失败'
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
    shopList.value = []
    pageNum.value = 1
    finished.value = false
    errorMsg.value = ''
  }

  async function reloadFeed() {
    resetFeed()
    await loadMore()
  }

  onMounted(async () => {
    await loadMore()
    await nextTick()
    bindObserver()
  })

  watch(sentinelRef, (el) => {
    if (!el || !observer) return
    observer.disconnect()
    observer.observe(el)
  })

  if (shopName) {
    watch(shopName, () => {
      reloadFeed()
    })
  }

  onUnmounted(() => {
    observer?.disconnect()
    observer = null
  })

  return {
    shopList,
    loading,
    finished,
    errorMsg,
    loadMore,
    retry,
    reloadFeed,
  }
}
