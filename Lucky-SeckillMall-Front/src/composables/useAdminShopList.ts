import { computed, onMounted, ref } from 'vue'
import { auditShop, getShopListByAdmin, updateShopStatus } from '@/api/shop'
import { formatDateTime } from '@/composables/useAdminUserList'
import type { ShopApplyQueryInput, ShopApplyVO } from '@/types/shop'
import {
  canUpdateShopStatus,
  formatShopStatusLabel,
  getToggleShopStatus,
  isShopEnabled,
  SHOP_QUERY_DEFAULT_PAGE_SIZE,
  ShopAuditStatus,
  ShopStatus,
} from '@/types/shop'

const DEFAULT_PAGE_SIZE = SHOP_QUERY_DEFAULT_PAGE_SIZE

const AUDIT_STATUS_LABELS: Record<ShopAuditStatus, string> = {
  [ShopAuditStatus.PENDING]: '待审核',
  [ShopAuditStatus.APPROVED]: '审核通过',
  [ShopAuditStatus.REJECTED]: '审核拒绝',
}

function toApiDateTime(value: string): string | undefined {
  if (!value.trim()) return undefined
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return undefined
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function formatAuditStatus(status: ShopAuditStatus, desc?: string): string {
  return desc || AUDIT_STATUS_LABELS[status] || '--'
}

export function formatShopStatus(status: ShopStatus | undefined, desc?: string): string {
  return formatShopStatusLabel(status, desc)
}

export function isShopPending(item: ShopApplyVO): boolean {
  return item.auditStatus === ShopAuditStatus.PENDING
}

export function isShopRejected(item: ShopApplyVO): boolean {
  return item.auditStatus === ShopAuditStatus.REJECTED
}

export function canAuditShop(item: ShopApplyVO): boolean {
  return !!item.id
}

export function canRejectShopAudit(item: ShopApplyVO): boolean {
  return item.auditStatus !== ShopAuditStatus.APPROVED
}

export function isShopApproved(item: ShopApplyVO): boolean {
  return item.auditStatus === ShopAuditStatus.APPROVED
}

export { isShopEnabled, canUpdateShopStatus, getToggleShopStatus }

export function useAdminShopList() {
  const loading = ref(false)
  const errorMsg = ref('')
  const toastMsg = ref('')
  const records = ref<ShopApplyVO[]>([])
  const pageNum = ref(1)
  const pageSize = ref(DEFAULT_PAGE_SIZE)
  const total = ref(0)
  const totalPages = ref(0)

  const filterUsername = ref('')
  const filterShopName = ref('')
  const filterAuditStatus = ref<ShopAuditStatus | ''>('')
  const filterShopStatus = ref<ShopStatus | ''>('')
  const filterStartTime = ref('')
  const filterEndTime = ref('')

  const showAuditModal = ref(false)
  const auditingShop = ref<ShopApplyVO | null>(null)
  const auditRemark = ref('')
  const auditSubmitting = ref(false)
  const auditErrorMsg = ref('')

  const showStatusConfirm = ref(false)
  const pendingShopId = ref('')
  const statusModalErrorMsg = ref('')
  const togglingId = ref<string | null>(null)

  const pendingStatusShop = computed(() =>
    records.value.find((item) => item.id === pendingShopId.value) ?? null,
  )

  const hasRecords = computed(() => records.value.length > 0)
  const hasPrevPage = computed(() => pageNum.value > 1)
  const hasNextPage = computed(() => pageNum.value < totalPages.value)

  function showToast(message: string) {
    toastMsg.value = message
    window.setTimeout(() => {
      if (toastMsg.value === message) {
        toastMsg.value = ''
      }
    }, 2000)
  }

  function buildQuery(): ShopApplyQueryInput {
    return {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: filterUsername.value,
      shopName: filterShopName.value,
      auditStatus: filterAuditStatus.value,
      shopStatus: filterShopStatus.value,
      startTime: toApiDateTime(filterStartTime.value),
      endTime: toApiDateTime(filterEndTime.value),
    }
  }

  async function loadList() {
    await fetchList(false)
  }

  async function refreshList() {
    await fetchList(true)
  }

  async function fetchList(silent: boolean) {
    if (!silent) {
      loading.value = true
    }
    errorMsg.value = ''
    try {
      const result = await getShopListByAdmin(buildQuery())
      records.value = result.records ?? []
      total.value = result.total ?? 0
      pageNum.value = result.pageNum ?? pageNum.value
      pageSize.value = result.pageSize ?? pageSize.value
      totalPages.value = result.totalPages ?? 0
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载店铺列表失败'
      records.value = []
      total.value = 0
      totalPages.value = 0
    } finally {
      if (!silent) {
        loading.value = false
      }
    }
  }

  function handleSearch() {
    pageNum.value = 1
    loadList()
  }

  function handleReset() {
    filterUsername.value = ''
    filterShopName.value = ''
    filterAuditStatus.value = ''
    filterShopStatus.value = ''
    filterStartTime.value = ''
    filterEndTime.value = ''
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

  function openAuditModal(shop: ShopApplyVO) {
    if (!canAuditShop(shop)) return
    auditingShop.value = shop
    auditRemark.value = shop.auditRemark?.trim() ?? ''
    auditErrorMsg.value = ''
    showAuditModal.value = true
  }

  function closeAuditModal() {
    if (auditSubmitting.value) return
    dismissAuditModal()
  }

  function dismissAuditModal() {
    showAuditModal.value = false
    auditingShop.value = null
    auditRemark.value = ''
    auditErrorMsg.value = ''
  }

  async function submitAudit(
    auditStatus: typeof ShopAuditStatus.APPROVED | typeof ShopAuditStatus.REJECTED,
  ) {
    const shop = auditingShop.value
    if (!shop || auditSubmitting.value) return

    if (shop.auditStatus === auditStatus) {
      auditErrorMsg.value = `当前已是「${formatAuditStatus(auditStatus)}」状态`
      return
    }

    if (auditStatus === ShopAuditStatus.REJECTED && !canRejectShopAudit(shop)) {
      auditErrorMsg.value = '审核通过后不允许改为拒绝'
      return
    }

    const remark = auditRemark.value.trim()
    if (auditStatus === ShopAuditStatus.REJECTED && !remark) {
      auditErrorMsg.value = '拒绝时请填写审核备注'
      return
    }

    auditSubmitting.value = true
    auditErrorMsg.value = ''
    try {
      await auditShop({
        shopId: shop.id,
        auditStatus,
        auditRemark: remark || undefined,
      })
      dismissAuditModal()
      showToast(auditStatus === ShopAuditStatus.APPROVED ? '审核已通过' : '审核已拒绝')
      void refreshList()
    } catch (err) {
      auditErrorMsg.value = err instanceof Error ? err.message : '审批失败'
    } finally {
      auditSubmitting.value = false
    }
  }

  function openStatusConfirm(shop: ShopApplyVO) {
    if (!canUpdateShopStatus(shop)) return
    pendingShopId.value = shop.id
    statusModalErrorMsg.value = ''
    showStatusConfirm.value = true
  }

  function closeStatusConfirm() {
    if (togglingId.value) return
    dismissStatusConfirm()
  }

  function dismissStatusConfirm() {
    showStatusConfirm.value = false
    pendingShopId.value = ''
    statusModalErrorMsg.value = ''
  }

  async function confirmToggleStatus() {
    const shopId = pendingShopId.value
    if (!shopId || togglingId.value) return

    const shop = records.value.find((item) => item.id === shopId)
    if (!shop) {
      statusModalErrorMsg.value = '店铺不存在或已被移除'
      return
    }

    const nextStatus = getToggleShopStatus(shop.shopStatus)
    togglingId.value = shopId
    statusModalErrorMsg.value = ''
    try {
      await updateShopStatus({ shopId, shopStatus: nextStatus })
      dismissStatusConfirm()
      showToast(nextStatus === ShopStatus.ENABLED ? '店铺已启用' : '店铺已禁用')
      void refreshList()
    } catch (err) {
      statusModalErrorMsg.value = err instanceof Error ? err.message : '更新店铺状态失败'
    } finally {
      togglingId.value = null
    }
  }

  onMounted(() => {
    loadList()
  })

  return {
    loading,
    errorMsg,
    toastMsg,
    records,
    pageNum,
    total,
    totalPages,
    filterUsername,
    filterShopName,
    filterAuditStatus,
    filterShopStatus,
    filterStartTime,
    filterEndTime,
    showAuditModal,
    auditingShop,
    auditRemark,
    auditSubmitting,
    auditErrorMsg,
    showStatusConfirm,
    pendingStatusShop,
    statusModalErrorMsg,
    togglingId,
    hasRecords,
    hasPrevPage,
    hasNextPage,
    ShopAuditStatus,
    ShopStatus,
    formatDateTime,
    formatAuditStatus,
    formatShopStatus,
    isShopPending,
    isShopApproved,
    isShopRejected,
    canAuditShop,
    canRejectShopAudit,
    isShopEnabled,
    loadList,
    handleSearch,
    handleReset,
    goPrevPage,
    goNextPage,
    openAuditModal,
    closeAuditModal,
    submitAudit,
    openStatusConfirm,
    closeStatusConfirm,
    confirmToggleStatus,
  }
}
