import { computed, onMounted, onUnmounted, ref } from 'vue'
import {
  listFailedOrders,
  manualDiscardFailedOrder,
  manualRetryFailedOrder,
} from '@/api/activity'
import { formatDateTime } from '@/composables/useAdminUserList'
import type { FailedOrderVO } from '@/types/activity'
import {
  canHandleFailedOrder,
  FailedOrderHandleStatus,
  formatFailedOrderHandleStatus,
} from '@/types/activity'

const PAGE_SIZE = 10
const PENDING_POLL_INTERVAL_MS = 15_000

export type FailedOrderTab = 'pending' | 'retried' | 'discarded'

function tabHandleStatus(tab: FailedOrderTab): FailedOrderHandleStatus {
  if (tab === 'pending') return FailedOrderHandleStatus.PENDING
  if (tab === 'retried') return FailedOrderHandleStatus.RETRIED
  return FailedOrderHandleStatus.DISCARDED
}

export function useAdminFailedOrders() {
  const loading = ref(false)
  const errorMsg = ref('')
  const toastMsg = ref('')
  const records = ref<FailedOrderVO[]>([])
  const pageNum = ref(1)
  const total = ref(0)
  const totalPages = ref(0)
  const pendingTotal = ref(0)
  const activeTab = ref<FailedOrderTab>('pending')
  const autoRefreshing = ref(false)
  const lastRefreshedAt = ref<Date | null>(null)

  // 重试确认弹窗
  const showRetryConfirm = ref(false)
  const retryingItem = ref<FailedOrderVO | null>(null)
  const retrySubmitting = ref(false)
  const retryErrorMsg = ref('')

  // 放弃弹窗
  const showDiscardDialog = ref(false)
  const discardingItem = ref<FailedOrderVO | null>(null)
  const discardRemark = ref('')
  const discardSubmitting = ref(false)
  const discardErrorMsg = ref('')

  const hasRecords = computed(() => records.value.length > 0)
  const hasPrevPage = computed(() => pageNum.value > 1)
  const hasNextPage = computed(() => pageNum.value < totalPages.value)

  const emptyTitle = computed(() => {
    if (activeTab.value === 'pending') return '暂无未处理的失败订单'
    if (activeTab.value === 'retried') return '暂无已重试的失败订单'
    return '暂无已放弃的失败订单'
  })

  function showToast(message: string) {
    toastMsg.value = message
    window.setTimeout(() => {
      if (toastMsg.value === message) {
        toastMsg.value = ''
      }
    }, 2000)
  }

  function formatRefreshTime(date: Date): string {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  }

  async function refreshPendingCount() {
    try {
      const result = await listFailedOrders({
        pageNum: 1,
        pageSize: 1,
        handleStatus: FailedOrderHandleStatus.PENDING,
      })
      pendingTotal.value = result.total ?? 0
    } catch {
      /* 角标刷新失败不影响主列表 */
    }
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
      const params = {
        handleStatus: tabHandleStatus(activeTab.value),
        pageNum: pageNum.value,
        pageSize: PAGE_SIZE,
      }

      const result = await listFailedOrders(params)

      records.value = result.records ?? []
      total.value = result.total ?? 0
      pageNum.value = result.pageNum ?? pageNum.value
      totalPages.value = result.totalPages ?? 0
      lastRefreshedAt.value = new Date()

      if (activeTab.value === 'pending') {
        pendingTotal.value = total.value
      }
    } catch (err) {
      if (!silent) {
        errorMsg.value = err instanceof Error ? err.message : '加载失败订单列表失败'
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

  function loadList() {
    return fetchList()
  }

  function switchTab(tab: FailedOrderTab) {
    if (activeTab.value === tab || loading.value) return
    activeTab.value = tab
    pageNum.value = 1
    void fetchList()
  }

  function goPrevPage() {
    if (!hasPrevPage.value || loading.value) return
    pageNum.value -= 1
    void fetchList()
  }

  function goNextPage() {
    if (!hasNextPage.value || loading.value) return
    pageNum.value += 1
    void fetchList()
  }

  // ======================== 重试 ========================

  function openRetryConfirm(item: FailedOrderVO) {
    if (!canHandleFailedOrder(item.handleStatus)) return
    retryingItem.value = item
    retryErrorMsg.value = ''
    showRetryConfirm.value = true
  }

  function closeRetryConfirm() {
    if (retrySubmitting.value) return
    dismissRetryConfirm()
  }

  function dismissRetryConfirm() {
    showRetryConfirm.value = false
    retryingItem.value = null
    retryErrorMsg.value = ''
  }

  async function confirmRetry() {
    const item = retryingItem.value
    if (!item || retrySubmitting.value) return

    retrySubmitting.value = true
    retryErrorMsg.value = ''
    try {
      await manualRetryFailedOrder(item.id)
      dismissRetryConfirm()
      showToast(`订单 ${item.orderNo || item.id} 已重新投递到主队列`)
      await fetchList()
      await refreshPendingCount()
    } catch (err) {
      retryErrorMsg.value = err instanceof Error ? err.message : '重试失败，请稍后再试'
    } finally {
      retrySubmitting.value = false
    }
  }

  // ======================== 放弃 ========================

  function openDiscardDialog(item: FailedOrderVO) {
    if (!canHandleFailedOrder(item.handleStatus)) return
    discardingItem.value = item
    discardRemark.value = ''
    discardErrorMsg.value = ''
    showDiscardDialog.value = true
  }

  function closeDiscardDialog() {
    if (discardSubmitting.value) return
    dismissDiscardDialog()
  }

  function dismissDiscardDialog() {
    showDiscardDialog.value = false
    discardingItem.value = null
    discardRemark.value = ''
    discardErrorMsg.value = ''
  }

  async function confirmDiscard() {
    const item = discardingItem.value
    if (!item || discardSubmitting.value) return

    discardSubmitting.value = true
    discardErrorMsg.value = ''
    try {
      await manualDiscardFailedOrder(item.id, discardRemark.value.trim() || undefined)
      dismissDiscardDialog()
      showToast(`订单 ${item.orderNo || item.id} 已标记为已放弃`)
      await fetchList()
      await refreshPendingCount()
    } catch (err) {
      discardErrorMsg.value = err instanceof Error ? err.message : '操作失败，请稍后再试'
    } finally {
      discardSubmitting.value = false
    }
  }

  // ======================== 轮询 ========================

  let pollTimer: ReturnType<typeof setInterval> | null = null

  function shouldPollPending() {
    return (
      activeTab.value === 'pending'
      && !loading.value
      && !autoRefreshing.value
      && !showRetryConfirm.value
      && !showDiscardDialog.value
      && !retrySubmitting.value
      && !discardSubmitting.value
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

  onMounted(() => {
    void fetchList()
    void refreshPendingCount()
    startPendingPoll()
    document.addEventListener('visibilitychange', handleVisibilityChange)
  })

  onUnmounted(() => {
    stopPendingPoll()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })

  return {
    loading,
    errorMsg,
    toastMsg,
    records,
    pageNum,
    total,
    totalPages,
    pendingTotal,
    activeTab,
    autoRefreshing,
    lastRefreshedAt,
    showRetryConfirm,
    retryingItem,
    retrySubmitting,
    retryErrorMsg,
    showDiscardDialog,
    discardingItem,
    discardRemark,
    discardSubmitting,
    discardErrorMsg,
    hasRecords,
    hasPrevPage,
    hasNextPage,
    emptyTitle,
    loadList,
    switchTab,
    goPrevPage,
    goNextPage,
    openRetryConfirm,
    closeRetryConfirm,
    confirmRetry,
    openDiscardDialog,
    closeDiscardDialog,
    confirmDiscard,
    formatDateTime,
    formatRefreshTime,
    formatFailedOrderHandleStatus,
    canHandleFailedOrder,
    FailedOrderHandleStatus,
  }
}
