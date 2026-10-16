import { computed, nextTick, onMounted, onUnmounted, ref, watch, type Ref } from 'vue'
import { getGoodsPage, getHomeGoodsList, searchGoods } from '@/api/goods'
import {
  buildCategoryGoodsQuery,
  buildGoodsSearchQuery,
  buildHomeGoodsListQuery,
  isCategoryFilterActive,
  type GoodsCategorySelection,
  type GoodsHomeSortType,
  type GoodsListVO,
} from '@/types/goods'

export type HomeGoodsFeedMode = 'home' | 'search' | 'category'

export function useHomeGoodsFeed(
  sentinelRef: Ref<HTMLElement | null>,
  options?: {
    goodsName?: Ref<string | undefined>
    categoryFilter?: Ref<GoodsCategorySelection | null | undefined>
    sortType?: Ref<GoodsHomeSortType>
  },
) {
  const goodsName = options?.goodsName
  const categoryFilter = options?.categoryFilter
  const sortType = options?.sortType ?? ref<GoodsHomeSortType>('weight')

  const goodsList = ref<GoodsListVO[]>([])
  const pageNum = ref(1)
  const loading = ref(false)
  const finished = ref(false)
  const errorMsg = ref('')

  const feedMode = computed<HomeGoodsFeedMode>(() => {
    if (categoryFilter?.value && isCategoryFilterActive(categoryFilter.value)) {
      return 'category'
    }
    if (goodsName?.value?.trim()) return 'search'
    return 'home'
  })

  const showHomeSort = computed(() => feedMode.value === 'home')

  async function fetchPage(currentPage: number) {
    if (feedMode.value === 'search') {
      const query = buildGoodsSearchQuery(currentPage, goodsName!.value!.trim())
      return searchGoods(query.goodsName ?? '', query.pageNum, query.pageSize)
    }
    if (feedMode.value === 'category') {
      return getGoodsPage(buildCategoryGoodsQuery(currentPage, categoryFilter!.value!))
    }
    return getHomeGoodsList(buildHomeGoodsListQuery(currentPage, sortType.value))
  }

  async function loadMore() {
    if (loading.value || finished.value) return

    loading.value = true
    errorMsg.value = ''

    const currentPage = pageNum.value

    try {
      const page = await fetchPage(currentPage)
      const records = page.records ?? []

      let freshRecords = records
      if (records.length) {
        const existingIds = new Set(goodsList.value.map((item) => item.id))
        freshRecords = records.filter((item) => !existingIds.has(item.id))
        goodsList.value.push(...freshRecords)
      }

      const pageTotal = Number(page.total ?? 0)
      const loadedCount = goodsList.value.length
      const totalPages = Number(page.totalPages ?? 0)
      const isLastPage =
        records.length === 0
        || (pageTotal > 0 && loadedCount >= pageTotal)
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
  }

  async function reloadFeed() {
    resetFeed()
    await loadMore()
  }

  function setSortType(type: GoodsHomeSortType) {
    if (!showHomeSort.value || sortType.value === type) return
    sortType.value = type
    reloadFeed()
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

  if (goodsName) {
    watch(goodsName, () => {
      reloadFeed()
    })
  }

  if (categoryFilter) {
    watch(
      categoryFilter,
      () => {
        reloadFeed()
      },
      { deep: true },
    )
  }

  onUnmounted(() => {
    observer?.disconnect()
    observer = null
  })

  return {
    goodsList,
    loading,
    finished,
    errorMsg,
    feedMode,
    showHomeSort,
    setSortType,
    loadMore,
    retry,
    reloadFeed,
  }
}
