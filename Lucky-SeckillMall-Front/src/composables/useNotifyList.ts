import { computed, onUnmounted, ref, watch } from 'vue'
import {
  getAdminNotifyList,
  getUserNotifyDetail,
  getUserNotifyList,
  getUserUnreadStatsByType,
  markAllAdminNotifyRead,
  markAllUserNotifyRead,
  markApplyNotifyRead,
  markNotifyRead,
} from '@/api/notify'
import {
  getNotifyListMode,
  getVisibleNotifyCategories,
  isAdminApplyNotifyType,
} from '@/constants/notify'
import type { Notify, NotifyListMode, NotifyReadFilter } from '@/types/notify'
import { useNotifyUnreadStats } from '@/composables/useNotifyUnreadStats'
import { dispatchNotifyChanged, NOTIFY_CHANGED_EVENT } from '@/utils/notifyEvent'
import { isAdmin } from '@/utils/auth'

const DEFAULT_PAGE_SIZE = 10

function applyReadState(item: Notify) {
  item.isRead = 1
  item.readTime = new Date().toISOString()
}

function toIsReadParam(filter: NotifyReadFilter): number | undefined {
  if (filter === 'unread') return 0
  if (filter === 'read') return 1
  return undefined
}

async function requestMarkRead(item: Notify, listMode: NotifyListMode) {
  if (listMode === 'admin' && isAdminApplyNotifyType(item.notifyType)) {
    const businessId = item.businessId != null ? String(item.businessId) : ''
    if (!businessId) {
      throw new Error('消息缺少业务 ID，无法标记已读')
    }
    await markApplyNotifyRead(businessId, item.notifyType)
    return
  }
  await markNotifyRead(item.id)
}

function formatMarkAllToast(markedCount: number) {
  if (markedCount <= 0) return '暂无未读消息'
  return `已将 ${markedCount} 条消息标记为已读`
}

export function useNotifyList(notifyType: () => number) {
  const listMode = computed(() => getNotifyListMode(notifyType(), isAdmin()))
  const isUserMode = computed(() => listMode.value === 'user')
  const { getUnreadByType, refreshUnreadStats } = useNotifyUnreadStats()

  const loading = ref(false)
  const errorMsg = ref('')
  const toastMsg = ref('')
  const records = ref<Notify[]>([])
  const pageNum = ref(1)
  const pageSize = ref(DEFAULT_PAGE_SIZE)
  const total = ref(0)
  const totalPages = ref(0)
  const typeUnreadCount = ref(0)
  const readFilter = ref<NotifyReadFilter>('all')
  const markingId = ref<string | null>(null)
  const markingAll = ref(false)
  const detailLoading = ref(false)
  const detailItem = ref<Notify | null>(null)
  const showDetail = ref(false)

  const hasRecords = computed(() => records.value.length > 0)
  const hasPrevPage = computed(() => pageNum.value > 1)
  const hasNextPage = computed(() => pageNum.value < totalPages.value)
  const pageUnreadCount = computed(() => records.value.filter(isUnreadNotify).length)
  const tabUnreadCount = computed(() => getUnreadByType(notifyType()))
  const hasUnread = computed(() => {
    if (isUserMode.value) return typeUnreadCount.value > 0
    return tabUnreadCount.value > 0 || pageUnreadCount.value > 0
  })

  function showToast(message: string) {
    toastMsg.value = message
    window.setTimeout(() => {
      if (toastMsg.value === message) {
        toastMsg.value = ''
      }
    }, 2000)
  }

  function syncReadSideEffects() {
    dispatchNotifyChanged()
  }

  async function loadList() {
    loading.value = true
    errorMsg.value = ''
    try {
      const query = {
        notifyType: notifyType(),
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        isRead: isUserMode.value ? toIsReadParam(readFilter.value) : undefined,
      }

      const result = listMode.value === 'admin'
        ? await getAdminNotifyList(query)
        : await getUserNotifyList(query)

      records.value = result.records ?? []
      total.value = result.total ?? 0
      pageNum.value = result.pageNum ?? pageNum.value
      pageSize.value = result.pageSize ?? pageSize.value
      totalPages.value = result.totalPages ?? 0
      typeUnreadCount.value = result.unreadCount ?? 0
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载消息失败'
      records.value = []
      total.value = 0
      totalPages.value = 0
      typeUnreadCount.value = 0
    } finally {
      loading.value = false
    }
  }

  function handleItemMarkedRead(item: Notify) {
    if (readFilter.value === 'unread') {
      records.value = records.value.filter((row) => row.id !== item.id)
      total.value = Math.max(0, total.value - 1)
    } else {
      const row = records.value.find((r) => r.id === item.id)
      if (row) applyReadState(row)
    }

    if (detailItem.value?.id === item.id) {
      applyReadState(detailItem.value)
    }

    if (typeUnreadCount.value > 0) {
      typeUnreadCount.value -= 1
    }
  }

  function setReadFilter(filter: NotifyReadFilter) {
    if (readFilter.value === filter) return
    readFilter.value = filter
    pageNum.value = 1
    loadList()
  }

  function goPrevPage() {
    if (!hasPrevPage.value || loading.value) return
    pageNum.value -= 1
    loadList()
  }

  function goNextPage() {
    if (!hasNextPage.value || loading.value) return
    pageNum.value += 1
    loadList()
  }

  async function markAsRead(item: Notify): Promise<boolean> {
    const notifyId = item.id
    if (!isUnreadNotify(item) || markingId.value === notifyId) return false
    if (!notifyId) {
      errorMsg.value = '消息 ID 无效，无法标记已读'
      return false
    }

    markingId.value = notifyId
    errorMsg.value = ''
    try {
      await requestMarkRead(item, listMode.value)
      handleItemMarkedRead(item)
      if (isUserMode.value) await refreshUnreadStats()
      syncReadSideEffects()
      showToast('已标记为已读')
      return true
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '标记已读失败'
      return false
    } finally {
      markingId.value = null
    }
  }

  function applyLocalAllReadState(markedCount: number) {
    typeUnreadCount.value = 0
    if (readFilter.value === 'unread') {
      records.value = []
      total.value = Math.max(0, total.value - markedCount)
    } else {
      records.value.forEach((item) => {
        if (isUnreadNotify(item)) applyReadState(item)
      })
    }
    if (detailItem.value && isUnreadNotify(detailItem.value)) {
      applyReadState(detailItem.value)
    }
  }

  async function markAllAsRead(markAllTypes = false) {
    if (markingAll.value) return
    if (!markAllTypes && !hasUnread.value) return

    markingAll.value = true
    errorMsg.value = ''

    try {
      const typeParam = markAllTypes ? undefined : notifyType()
      const result = listMode.value === 'admin'
        ? await markAllAdminNotifyRead(typeParam)
        : await markAllUserNotifyRead(typeParam)

      const markedCount = result?.markedCount ?? 0
      applyLocalAllReadState(markedCount)
      await refreshUnreadStats()
      syncReadSideEffects()
      showToast(formatMarkAllToast(markedCount))

      if (markedCount > 0) {
        await loadList()
      }
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '批量标记已读失败'
    } finally {
      markingAll.value = false
    }
  }

  async function openDetail(item: Notify) {
    if (!isUserMode.value) {
      if (isUnreadNotify(item)) await markAsRead(item)
      return
    }

    showDetail.value = true
    detailItem.value = item
    detailLoading.value = true
    errorMsg.value = ''

    try {
      const detail = await getUserNotifyDetail(item.id)
      detailItem.value = detail
      const index = records.value.findIndex((row) => row.id === item.id)
      if (index >= 0) {
        records.value[index] = { ...records.value[index], ...detail }
      }
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载消息详情失败'
    } finally {
      detailLoading.value = false
    }
  }

  function closeDetail() {
    showDetail.value = false
    detailItem.value = null
  }

  function getTabUnreadCount(type: number): number {
    return getUnreadByType(type)
  }

  function handleNotifyChanged() {
    if (loading.value) return
    void loadList()
  }

  watch(
    notifyType,
    () => {
      pageNum.value = 1
      readFilter.value = 'all'
      loadList()
    },
    { immediate: true },
  )

  if (typeof window !== 'undefined') {
    window.addEventListener(NOTIFY_CHANGED_EVENT, handleNotifyChanged)
  }

  onUnmounted(() => {
    if (typeof window !== 'undefined') {
      window.removeEventListener(NOTIFY_CHANGED_EVENT, handleNotifyChanged)
    }
  })

  return {
    loading,
    errorMsg,
    toastMsg,
    records,
    pageNum,
    total,
    totalPages,
    typeUnreadCount,
    readFilter,
    hasRecords,
    hasPrevPage,
    hasNextPage,
    pageUnreadCount,
    tabUnreadCount,
    hasUnread,
    isUserMode,
    markingId,
    markingAll,
    detailLoading,
    detailItem,
    showDetail,
    loadList,
    setReadFilter,
    goPrevPage,
    goNextPage,
    markAsRead,
    markAllAsRead,
    openDetail,
    closeDetail,
    getTabUnreadCount,
    isUnreadNotify,
  }
}

export function formatNotifyTime(value?: string): string {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function isUnreadNotify(item: Notify): boolean {
  return item.isRead !== 1
}

/** 加载用户未读消息预览（首页浮层 / 顶栏一致） */
export async function loadUserNotifyPreview(pageSize = 8, notifyType?: number) {
  const result = await getUserNotifyList({
    notifyType,
    pageNum: 1,
    pageSize,
    isRead: 0,
  })
  return result.records ?? []
}

/** 加载未读消息预览（用户 / 管理员，与顶栏消息中心一致） */
export async function loadNotifyPreview(pageSize = 8) {
  if (!isAdmin()) {
    return loadUserNotifyPreview(pageSize)
  }

  const categories = getVisibleNotifyCategories(true)
  const perType = Math.max(4, Math.ceil(pageSize / categories.length))
  const batches = await Promise.all(
    categories.map(async (cat) => {
      try {
        const page = await getAdminNotifyList({
          notifyType: cat.type,
          pageNum: 1,
          pageSize: perType * 4,
        })
        return (page.records ?? []).filter(isUnreadNotify)
      } catch {
        return []
      }
    }),
  )

  return batches
    .flat()
    .sort((a, b) => Date.parse(b.createTime) - Date.parse(a.createTime))
    .slice(0, pageSize)
}

/** 查询当前用户总未读数（各类型未读之和，与 useNotifyUnreadStats 一致） */
export async function loadUserUnreadTotal() {
  if (isAdmin()) {
    const categories = getVisibleNotifyCategories(true)
    let total = 0
    for (const cat of categories) {
      try {
        const page = await getAdminNotifyList({
          notifyType: cat.type,
          pageNum: 1,
          pageSize: 100,
        })
        total += (page.records ?? []).filter(isUnreadNotify).length
      } catch {
        // 忽略单类加载失败
      }
    }
    return total
  }
  const stats = await getUserUnreadStatsByType()
  return stats.reduce((sum, item) => sum + (item.unreadCount ?? 0), 0)
}
