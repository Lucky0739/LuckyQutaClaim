import { computed, onMounted, ref } from 'vue'
import { endActivity, getActivityList, startActivity } from '@/api/activity'
import { formatDateTime } from '@/composables/useAdminUserList'
import type { SeckillActivityVO } from '@/types/activity'
import {
  canEndActivity,
  canStartActivity,
  formatActivityStatus,
  isActivityEnded,
  isActivityInProgress,
  SeckillActivityStatus,
} from '@/types/activity'

export type ActivityActionType = 'start' | 'end'

export function useAdminActivityList() {
  const loading = ref(false)
  const errorMsg = ref('')
  const toastMsg = ref('')
  const records = ref<SeckillActivityVO[]>([])
  const actingId = ref<string | null>(null)
  const actingType = ref<ActivityActionType | null>(null)

  const showActionConfirm = ref(false)
  const pendingActivity = ref<SeckillActivityVO | null>(null)
  const pendingAction = ref<ActivityActionType | null>(null)
  const actionErrorMsg = ref('')

  const hasRecords = computed(() => records.value.length > 0)
  const isBusy = computed(() => loading.value || actingId.value !== null)

  const stats = computed(() => {
    const total = records.value.length
    const notStarted = records.value.filter(
      (item) => item.status === SeckillActivityStatus.NOT_STARTED,
    ).length
    const inProgress = records.value.filter(
      (item) => item.status === SeckillActivityStatus.IN_PROGRESS,
    ).length
    const ended = records.value.filter((item) => item.status === SeckillActivityStatus.ENDED).length
    return { total, notStarted, inProgress, ended }
  })

  function showToast(message: string) {
    toastMsg.value = message
    window.setTimeout(() => {
      if (toastMsg.value === message) {
        toastMsg.value = ''
      }
    }, 2000)
  }

  async function loadList() {
    loading.value = true
    errorMsg.value = ''
    try {
      records.value = await getActivityList()
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载秒杀活动列表失败'
      records.value = []
    } finally {
      loading.value = false
    }
  }

  function openActionConfirm(activity: SeckillActivityVO, action: ActivityActionType) {
    if (isBusy.value) return
    pendingActivity.value = activity
    pendingAction.value = action
    actionErrorMsg.value = ''
    showActionConfirm.value = true
  }

  function closeActionConfirm() {
    if (actingId.value !== null) return
    dismissActionConfirm()
  }

  function dismissActionConfirm() {
    showActionConfirm.value = false
    pendingActivity.value = null
    pendingAction.value = null
    actionErrorMsg.value = ''
  }

  function updateRecord(updated: SeckillActivityVO) {
    const index = records.value.findIndex((item) => item.id === updated.id)
    if (index >= 0) {
      records.value[index] = updated
    }
  }

  async function confirmAction() {
    const activity = pendingActivity.value
    const action = pendingAction.value
    if (!activity || !action || actingId.value !== null) return

    actingId.value = activity.id
    actingType.value = action
    actionErrorMsg.value = ''

    try {
      if (action === 'start') {
        const updated = await startActivity(activity.id)
        updateRecord(updated)
        showToast('秒杀活动已开始，库存已预热')
      } else {
        await endActivity(activity.id)
        updateRecord({ ...activity, status: SeckillActivityStatus.ENDED })
        showToast('秒杀活动已结束')
      }
      dismissActionConfirm()
    } catch (err) {
      actionErrorMsg.value = err instanceof Error ? err.message : '操作失败，请稍后重试'
    } finally {
      actingId.value = null
      actingType.value = null
    }
  }

  function openStartConfirm(activity: SeckillActivityVO) {
    openActionConfirm(activity, 'start')
  }

  function openEndConfirm(activity: SeckillActivityVO) {
    openActionConfirm(activity, 'end')
  }

  onMounted(() => {
    void loadList()
  })

  return {
    loading,
    errorMsg,
    toastMsg,
    records,
    actingId,
    actingType,
    showActionConfirm,
    pendingActivity,
    pendingAction,
    actionErrorMsg,
    hasRecords,
    isBusy,
    stats,
    loadList,
    openStartConfirm,
    openEndConfirm,
    closeActionConfirm,
    confirmAction,
    canStartActivity,
    canEndActivity,
    formatActivityStatus,
    formatDateTime,
    isActivityEnded,
    isActivityInProgress,
    SeckillActivityStatus,
  }
}
