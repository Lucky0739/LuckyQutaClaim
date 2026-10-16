import { computed, onMounted, onUnmounted, ref, shallowRef } from 'vue'
import { getAdminNotifyList, getUserUnreadStatsByType } from '@/api/notify'
import { getVisibleNotifyCategories } from '@/constants/notify'
import type { NotifyUnreadStatVO } from '@/types/notify'
import { NOTIFY_CHANGED_EVENT } from '@/utils/notifyEvent'
import { isAdmin, isLoggedIn } from '@/utils/auth'

const unreadStats = shallowRef<NotifyUnreadStatVO[]>([])
const loading = ref(false)
let subscriberCount = 0

export function formatNotifyBadgeCount(count: number) {
  return count > 99 ? '99+' : String(count)
}

async function fetchAdminUnreadStats(): Promise<NotifyUnreadStatVO[]> {
  const categories = getVisibleNotifyCategories(true)
  const batches = await Promise.all(
    categories.map(async (cat) => {
      try {
        const page = await getAdminNotifyList({
          notifyType: cat.type,
          pageNum: 1,
          pageSize: 100,
        })
        const unreadCount = (page.records ?? []).filter((item) => item.isRead !== 1).length
        return {
          notifyType: cat.type,
          notifyTypeName: cat.label,
          unreadCount,
        } satisfies NotifyUnreadStatVO
      } catch {
        return {
          notifyType: cat.type,
          notifyTypeName: cat.label,
          unreadCount: 0,
        }
      }
    }),
  )
  return batches
}

async function fetchUnreadStats() {
  if (!isLoggedIn()) {
    unreadStats.value = []
    return
  }
  loading.value = true
  try {
    unreadStats.value = isAdmin()
      ? await fetchAdminUnreadStats()
      : await getUserUnreadStatsByType()
  } catch {
    unreadStats.value = []
  } finally {
    loading.value = false
  }
}

/** 各消息类型未读数（侧栏、Tab、顶栏角标共用） */
export function useNotifyUnreadStats() {
  const statsMap = computed(() => {
    const map = new Map<number, number>()
    for (const item of unreadStats.value) {
      map.set(item.notifyType, item.unreadCount ?? 0)
    }
    return map
  })

  const totalUnread = computed(() =>
    unreadStats.value.reduce((sum, item) => sum + (item.unreadCount ?? 0), 0),
  )

  function getUnreadByType(type: number) {
    return statsMap.value.get(type) ?? 0
  }

  onMounted(() => {
    subscriberCount += 1
    if (subscriberCount === 1) {
      void fetchUnreadStats()
      window.addEventListener(NOTIFY_CHANGED_EVENT, fetchUnreadStats)
    }
  })

  onUnmounted(() => {
    subscriberCount -= 1
    if (subscriberCount === 0) {
      window.removeEventListener(NOTIFY_CHANGED_EVENT, fetchUnreadStats)
    }
  })

  return {
    unreadStats,
    loading,
    totalUnread,
    getUnreadByType,
    formatBadgeCount: formatNotifyBadgeCount,
    refreshUnreadStats: fetchUnreadStats,
  }
}
