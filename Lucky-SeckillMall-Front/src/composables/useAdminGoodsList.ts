import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { auditGoods, getAuditedGoodsPage, getPendingAuditGoodsPage } from '@/api/goods'
import { formatDateTime } from '@/composables/useAdminUserList'
import type { GoodsListVO, GoodsQueryRequest } from '@/types/goods'
import {
  canAuditGoods,
  formatGoodsAuditStatus,
  formatGoodsCategory,
  formatGoodsPrice,
  formatGoodsSalesNum,
  GoodsAuditStatus,
} from '@/types/goods'

const DEFAULT_PAGE_SIZE = 10
/** 待审核列表自动刷新间隔（毫秒） */
const PENDING_POLL_INTERVAL_MS = 10_000

export type AdminGoodsTab = 'pending' | 'passed' | 'rejected'

function toApiDateTime(value: string): string | undefined {
  if (!value.trim()) return undefined
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return undefined
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function useAdminGoodsList() {
  const route = useRoute()
  const loading = ref(false)
  const pendingTotal = ref(0)
  const errorMsg = ref('')
  const toastMsg = ref('')
  const records = ref<GoodsListVO[]>([])
  const pageNum = ref(1)
  const pageSize = ref(DEFAULT_PAGE_SIZE)
  const total = ref(0)
  const totalPages = ref(0)

  const activeTab = ref<AdminGoodsTab>('pending')
  const filterGoodsName = ref('')
  const filterStartTime = ref('')
  const filterEndTime = ref('')

  const showAuditModal = ref(false)
  const auditingGoods = ref<GoodsListVO | null>(null)
  const auditRemark = ref('')
  const auditSubmitting = ref(false)
  const auditErrorMsg = ref('')
  const autoRefreshing = ref(false)
  const lastRefreshedAt = ref<Date | null>(null)

  const hasRecords = computed(() => records.value.length > 0)
  const showPendingAutoRefresh = computed(() => activeTab.value === 'pending')
  const hasPrevPage = computed(() => pageNum.value > 1)
  const hasNextPage = computed(() => pageNum.value < totalPages.value)

  const tabStats = computed(() => ({
    pending: pendingTotal.value,
    passed: activeTab.value === 'passed' ? total.value : null,
    rejected: activeTab.value === 'rejected' ? total.value : null,
  }))

  function showToast(message: string) {
    toastMsg.value = message
    window.setTimeout(() => {
      if (toastMsg.value === message) {
        toastMsg.value = ''
      }
    }, 2000)
  }

  function buildQuery(): GoodsQueryRequest {
    const query: GoodsQueryRequest = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }

    const goodsName = filterGoodsName.value.trim()
    if (goodsName) query.goodsName = goodsName

    const startTime = toApiDateTime(filterStartTime.value)
    if (startTime) query.startTime = startTime

    const endTime = toApiDateTime(filterEndTime.value)
    if (endTime) query.endTime = endTime

    if (activeTab.value === 'passed') {
      query.auditStatus = GoodsAuditStatus.PASS
    } else if (activeTab.value === 'rejected') {
      query.auditStatus = GoodsAuditStatus.REJECT
    }

    return query
  }

  async function fetchList(options?: { silent?: boolean }) {
    const silent = options?.silent === true
    if (!silent) {
      loading.value = true
      errorMsg.value = ''
    } else {
      autoRefreshing.value = true
    }

    try {
      const query = buildQuery()
      const result =
        activeTab.value === 'pending'
          ? await getPendingAuditGoodsPage(query)
          : await getAuditedGoodsPage(query)

      records.value = result.records ?? []
      total.value = result.total ?? 0
      pageNum.value = result.pageNum ?? pageNum.value
      pageSize.value = result.pageSize ?? pageSize.value
      totalPages.value = result.totalPages ?? 0
      lastRefreshedAt.value = new Date()
      if (activeTab.value === 'pending') {
        pendingTotal.value = total.value
      }
    } catch (err) {
      if (!silent) {
        errorMsg.value = err instanceof Error ? err.message : '加载商品列表失败'
        records.value = []
        total.value = 0
        totalPages.value = 0
      }
    } finally {
      if (!silent) {
        loading.value = false
      } else {
        autoRefreshing.value = false
      }
    }
  }

  function switchTab(tab: AdminGoodsTab) {
    if (activeTab.value === tab || loading.value) return
    activeTab.value = tab
    pageNum.value = 1
    fetchList()
  }

  async function refreshPendingCount() {
    try {
      const result = await getPendingAuditGoodsPage({ pageNum: 1, pageSize: 1 })
      pendingTotal.value = result.total ?? 0
    } catch {
      /* 角标刷新失败不影响主列表 */
    }
  }

  function loadList() {
    return fetchList()
  }

  async function tryOpenAuditFromRoute() {
    const goodsId = String(route.query.goodsId ?? '').trim()
    if (!goodsId) return

    if (activeTab.value !== 'pending') {
      activeTab.value = 'pending'
      pageNum.value = 1
      await fetchList()
    }

    const goods = records.value.find((item) => item.id === goodsId)
    if (goods && canAuditGoods(goods.auditStatus)) {
      openAuditModal(goods)
    }
  }

  function handleSearch() {
    pageNum.value = 1
    fetchList()
  }

  function handleReset() {
    filterGoodsName.value = ''
    filterStartTime.value = ''
    filterEndTime.value = ''
    pageNum.value = 1
    fetchList()
  }

  function goPrevPage() {
    if (!hasPrevPage.value || loading.value) return
    pageNum.value -= 1
    fetchList()
  }

  function goNextPage() {
    if (!hasNextPage.value || loading.value) return
    pageNum.value += 1
    fetchList()
  }

  function openAuditModal(goods: GoodsListVO) {
    if (!canAuditGoods(goods.auditStatus)) return
    auditingGoods.value = goods
    auditRemark.value = ''
    auditErrorMsg.value = ''
    showAuditModal.value = true
  }

  function closeAuditModal() {
    if (auditSubmitting.value) return
    dismissAuditModal()
  }

  function dismissAuditModal() {
    showAuditModal.value = false
    auditingGoods.value = null
    auditRemark.value = ''
    auditErrorMsg.value = ''
  }

  async function submitAudit(auditStatus: typeof GoodsAuditStatus.PASS | typeof GoodsAuditStatus.REJECT) {
    const goods = auditingGoods.value
    if (!goods || auditSubmitting.value) return

    const remark = auditRemark.value.trim()
    if (auditStatus === GoodsAuditStatus.REJECT && !remark) {
      auditErrorMsg.value = '拒绝审核时请填写审核备注'
      return
    }

    auditSubmitting.value = true
    auditErrorMsg.value = ''
    try {
      await auditGoods({
        goodsId: goods.id,
        auditStatus,
        auditRemark: remark || undefined,
      })
      dismissAuditModal()
      showToast(auditStatus === GoodsAuditStatus.PASS ? '商品审核已通过' : '商品审核已拒绝')
      await fetchList()
      await refreshPendingCount()
    } catch (err) {
      auditErrorMsg.value = err instanceof Error ? err.message : '审核操作失败'
    } finally {
      auditSubmitting.value = false
    }
  }

  let pollTimer: ReturnType<typeof setInterval> | null = null

  function shouldPollPending() {
    return (
      activeTab.value === 'pending'
      && !loading.value
      && !autoRefreshing.value
      && !showAuditModal.value
      && !auditSubmitting.value
      && document.visibilityState === 'visible'
    )
  }

  function stopPendingPoll() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  function startPendingPoll() {
    stopPendingPoll()
    pollTimer = setInterval(() => {
      if (document.visibilityState !== 'visible') return
      void refreshPendingCount()
      if (shouldPollPending()) {
        void fetchList({ silent: true })
      }
    }, PENDING_POLL_INTERVAL_MS)
  }

  function handleVisibilityChange() {
    if (document.visibilityState !== 'visible') return
    void refreshPendingCount()
    if (activeTab.value === 'pending') {
      void fetchList({ silent: true })
    }
  }

  onMounted(async () => {
    await Promise.all([fetchList(), refreshPendingCount()])
    startPendingPoll()
    document.addEventListener('visibilitychange', handleVisibilityChange)
    await tryOpenAuditFromRoute()
  })

  watch(
    () => route.query.goodsId,
    () => {
      void tryOpenAuditFromRoute()
    },
  )

  onUnmounted(() => {
    stopPendingPoll()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })

  return {
    loading,
    errorMsg,
    toastMsg,
    pendingTotal,
    records,
    pageNum,
    total,
    totalPages,
    activeTab,
    filterGoodsName,
    filterStartTime,
    filterEndTime,
    showAuditModal,
    auditingGoods,
    auditRemark,
    auditSubmitting,
    auditErrorMsg,
    autoRefreshing,
    lastRefreshedAt,
    showPendingAutoRefresh,
    hasRecords,
    hasPrevPage,
    hasNextPage,
    tabStats,
    loadList,
    switchTab,
    handleSearch,
    handleReset,
    goPrevPage,
    goNextPage,
    openAuditModal,
    closeAuditModal,
    submitAudit,
    canAuditGoods,
    formatGoodsAuditStatus,
    formatGoodsCategory,
    formatGoodsPrice,
    formatGoodsSalesNum,
    formatDateTime,
    GoodsAuditStatus,
  }
}
