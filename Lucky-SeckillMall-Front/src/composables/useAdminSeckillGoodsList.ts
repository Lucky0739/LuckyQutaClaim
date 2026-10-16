import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { auditSeckillGoods, getActivityList, listSeckillGoodsApply } from '@/api/activity'
import { formatDateTime } from '@/composables/useAdminUserList'
import type { SeckillActivityVO, SeckillGoodsApplyVO } from '@/types/activity'
import {
  canAuditSeckillGoods,
  formatSeckillGoodsAuditStatus,
  SeckillGoodsAuditStatus,
} from '@/types/activity'
import { notifySeckillDisplayChanged } from '@/utils/seckillEvents'

const PAGE_SIZE = 10
const PENDING_POLL_INTERVAL_MS = 10_000

export type AdminSeckillGoodsTab = 'pending' | 'approved' | 'rejected'

function tabAuditStatus(tab: AdminSeckillGoodsTab): SeckillGoodsAuditStatus | undefined {
  if (tab === 'pending') return SeckillGoodsAuditStatus.PENDING
  if (tab === 'approved') return SeckillGoodsAuditStatus.APPROVED
  return SeckillGoodsAuditStatus.REJECTED
}

export function useAdminSeckillGoodsList() {
  const route = useRoute()

  const loading = ref(false)
  const errorMsg = ref('')
  const toastMsg = ref('')
  const records = ref<SeckillGoodsApplyVO[]>([])
  const activities = ref<SeckillActivityVO[]>([])
  const pageNum = ref(1)
  const total = ref(0)
  const totalPages = ref(0)
  const pendingTotal = ref(0)
  const activeTab = ref<AdminSeckillGoodsTab>('pending')
  const filterActivityId = ref('')
  const autoRefreshing = ref(false)
  const lastRefreshedAt = ref<Date | null>(null)

  const showAuditModal = ref(false)
  const auditingItem = ref<SeckillGoodsApplyVO | null>(null)
  const auditRemark = ref('')
  const auditSubmitting = ref(false)
  const auditErrorMsg = ref('')

  const hasRecords = computed(() => records.value.length > 0)
  const hasPrevPage = computed(() => pageNum.value > 1)
  const hasNextPage = computed(() => pageNum.value < totalPages.value)
  const showPendingAutoRefresh = computed(() => activeTab.value === 'pending')

  const emptyTitle = computed(() => {
    if (activeTab.value === 'pending') return '暂无待审核的秒杀商品申请'
    if (activeTab.value === 'approved') return '暂无已通过的秒杀商品申请'
    return '暂无已拒绝的秒杀商品申请'
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
      const result = await listSeckillGoodsApply({
        pageNum: 1,
        pageSize: 1,
        auditStatus: SeckillGoodsAuditStatus.PENDING,
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
      const result = await listSeckillGoodsApply({
        pageNum: pageNum.value,
        pageSize: PAGE_SIZE,
        activityId: filterActivityId.value || undefined,
        auditStatus: tabAuditStatus(activeTab.value),
      })

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
        errorMsg.value = err instanceof Error ? err.message : '加载秒杀商品申请失败'
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

  async function loadActivities() {
    try {
      activities.value = await getActivityList()
    } catch {
      activities.value = []
    }
  }

  function loadList() {
    return fetchList()
  }

  function switchTab(tab: AdminSeckillGoodsTab) {
    if (activeTab.value === tab || loading.value) return
    activeTab.value = tab
    pageNum.value = 1
    void fetchList()
  }

  function handleSearch() {
    pageNum.value = 1
    void fetchList()
  }

  function handleReset() {
    filterActivityId.value = ''
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

  function openAuditModal(item: SeckillGoodsApplyVO) {
    if (!canAuditSeckillGoods(item.auditStatus)) return
    auditingItem.value = item
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
    auditingItem.value = null
    auditRemark.value = ''
    auditErrorMsg.value = ''
  }

  async function submitAudit(
    auditStatus:
      | typeof SeckillGoodsAuditStatus.APPROVED
      | typeof SeckillGoodsAuditStatus.REJECTED,
  ) {
    const item = auditingItem.value
    if (!item || auditSubmitting.value) return

    const remark = auditRemark.value.trim()
    if (auditStatus === SeckillGoodsAuditStatus.REJECTED && !remark) {
      auditErrorMsg.value = '拒绝审核时请填写审核备注'
      return
    }

    auditSubmitting.value = true
    auditErrorMsg.value = ''
    try {
      await auditSeckillGoods({
        seckillGoodsId: item.id,
        auditStatus,
        auditRemark: remark || undefined,
      })
      dismissAuditModal()
      showToast(auditStatus === SeckillGoodsAuditStatus.APPROVED ? '秒杀商品审核已通过' : '秒杀商品审核已拒绝')
      if (auditStatus === SeckillGoodsAuditStatus.APPROVED) {
        notifySeckillDisplayChanged()
      }
      await fetchList()
      await refreshPendingCount()
    } catch (err) {
      auditErrorMsg.value = err instanceof Error ? err.message : '审核操作失败'
    } finally {
      auditSubmitting.value = false
    }
  }

  async function tryOpenAuditFromRoute() {
    const seckillGoodsId = String(route.query.seckillGoodsId ?? '').trim()
    if (!seckillGoodsId) return

    if (activeTab.value !== 'pending') {
      activeTab.value = 'pending'
      pageNum.value = 1
      await fetchList()
    }

    const item = records.value.find((record) => record.id === seckillGoodsId)
    if (item && canAuditSeckillGoods(item.auditStatus)) {
      openAuditModal(item)
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
    await loadActivities()
    await Promise.all([fetchList(), refreshPendingCount()])
    startPendingPoll()
    document.addEventListener('visibilitychange', handleVisibilityChange)
    await tryOpenAuditFromRoute()
  })

  watch(
    () => route.query.seckillGoodsId,
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
    records,
    activities,
    pageNum,
    total,
    totalPages,
    pendingTotal,
    activeTab,
    filterActivityId,
    autoRefreshing,
    lastRefreshedAt,
    showAuditModal,
    auditingItem,
    auditRemark,
    auditSubmitting,
    auditErrorMsg,
    hasRecords,
    hasPrevPage,
    hasNextPage,
    showPendingAutoRefresh,
    emptyTitle,
    loadList,
    switchTab,
    handleSearch,
    handleReset,
    goPrevPage,
    goNextPage,
    openAuditModal,
    closeAuditModal,
    submitAudit,
    formatDateTime,
    formatRefreshTime,
    formatSeckillGoodsAuditStatus,
    SeckillGoodsAuditStatus,
  }
}
