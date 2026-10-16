import { computed, onMounted, ref } from 'vue'
import { adminDeleteUser, adminUpdateUserProfile, getUserListByAdmin, updateUserStatus } from '@/api/user'
import type { AdminUserListVO, AdminUserQueryRequest } from '@/types/user'
import { UserStatus } from '@/types/user'
import { getUserInfo } from '@/utils/auth'

const DEFAULT_PAGE_SIZE = 10

const ROLE_LABELS: Record<string, string> = {
  admin: '管理员',
  merchant: '商户',
}

export function formatDateTime(value?: string): string {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function formatRoleName(role: string): string {
  return ROLE_LABELS[role] ?? role
}

function toApiDateTime(value: string): string | undefined {
  if (!value.trim()) return undefined
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return undefined
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function isUserEnabled(status: UserStatus): boolean {
  return status === UserStatus.ENABLED
}

export function isTargetAdminUser(user: AdminUserListVO): boolean {
  return user.roleNames?.includes('admin') ?? false
}

function getCurrentUserId(): string {
  const id = getUserInfo()?.id
  return id != null ? String(id) : ''
}

export function canDeleteUser(user: AdminUserListVO): boolean {
  if (user.id === getCurrentUserId()) return false
  if (isTargetAdminUser(user)) return false
  return true
}

export function getDeleteDisabledReason(user: AdminUserListVO): string | null {
  if (user.id === getCurrentUserId()) return '不能删除自己的账号'
  if (isTargetAdminUser(user)) return '不能删除管理员账号'
  return null
}

export function useAdminUserList() {
  const loading = ref(false)
  const errorMsg = ref('')
  const toastMsg = ref('')
  const records = ref<AdminUserListVO[]>([])
  const pageNum = ref(1)
  const pageSize = ref(DEFAULT_PAGE_SIZE)
  const total = ref(0)
  const totalPages = ref(0)

  const filterNickname = ref('')
  const filterMobile = ref('')
  const filterStatus = ref<UserStatus | ''>('')
  const filterStartTime = ref('')
  const filterEndTime = ref('')

  const showEditModal = ref(false)
  const editingUser = ref<AdminUserListVO | null>(null)
  const editNickname = ref('')
  const editSubmitting = ref(false)
  const editErrorMsg = ref('')

  const showStatusConfirm = ref(false)
  const pendingStatusUser = ref<AdminUserListVO | null>(null)
  const togglingId = ref<string | null>(null)

  const showDeleteConfirm = ref(false)
  const pendingDeleteUser = ref<AdminUserListVO | null>(null)
  const deletingId = ref<string | null>(null)

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

  function buildQuery(): AdminUserQueryRequest {
    const query: AdminUserQueryRequest = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }

    const nickname = filterNickname.value.trim()
    if (nickname) query.nickname = nickname

    const mobile = filterMobile.value.trim()
    if (mobile) query.mobile = mobile

    if (filterStatus.value !== '') {
      query.status = filterStatus.value
    }

    const startTime = toApiDateTime(filterStartTime.value)
    if (startTime) query.startTime = startTime

    const endTime = toApiDateTime(filterEndTime.value)
    if (endTime) query.endTime = endTime

    return query
  }

  async function loadList() {
    loading.value = true
    errorMsg.value = ''
    try {
      const result = await getUserListByAdmin(buildQuery())
      records.value = result.records ?? []
      total.value = result.total ?? 0
      pageNum.value = result.pageNum ?? pageNum.value
      pageSize.value = result.pageSize ?? pageSize.value
      totalPages.value = result.totalPages ?? 0
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载用户列表失败'
      records.value = []
      total.value = 0
      totalPages.value = 0
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    pageNum.value = 1
    loadList()
  }

  function handleReset() {
    filterNickname.value = ''
    filterMobile.value = ''
    filterStatus.value = ''
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

  function openEditModal(user: AdminUserListVO) {
    editingUser.value = user
    editNickname.value = user.nickname
    editErrorMsg.value = ''
    showEditModal.value = true
  }

  function closeEditModal() {
    if (editSubmitting.value) return
    showEditModal.value = false
    editingUser.value = null
    editNickname.value = ''
    editErrorMsg.value = ''
  }

  async function submitEdit() {
    const user = editingUser.value
    if (!user || editSubmitting.value) return

    const nickname = editNickname.value.trim()
    if (!nickname) {
      editErrorMsg.value = '昵称不能为空'
      return
    }
    if (nickname === user.nickname) {
      closeEditModal()
      return
    }

    editSubmitting.value = true
    editErrorMsg.value = ''
    try {
      await adminUpdateUserProfile({ userId: user.id, nickname })
      user.nickname = nickname
      showToast('昵称修改成功')
      editSubmitting.value = false
      closeEditModal()
    } catch (err) {
      editErrorMsg.value = err instanceof Error ? err.message : '修改昵称失败'
    } finally {
      editSubmitting.value = false
    }
  }

  function openStatusConfirm(user: AdminUserListVO) {
    pendingStatusUser.value = user
    showStatusConfirm.value = true
  }

  function closeStatusConfirm() {
    if (togglingId.value) return
    dismissStatusConfirm()
  }

  function dismissStatusConfirm() {
    showStatusConfirm.value = false
    pendingStatusUser.value = null
  }

  async function confirmToggleStatus() {
    const user = pendingStatusUser.value
    if (!user || togglingId.value) return

    const nextStatus = isUserEnabled(user.status) ? UserStatus.DISABLED : UserStatus.ENABLED
    togglingId.value = user.id
    errorMsg.value = ''
    try {
      await updateUserStatus({ userId: user.id, status: nextStatus })
      user.status = nextStatus
      user.statusDesc = nextStatus === UserStatus.ENABLED ? '启用' : '禁用'
      showToast(nextStatus === UserStatus.ENABLED ? '用户已启用' : '用户已禁用')
      dismissStatusConfirm()
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '更新用户状态失败'
    } finally {
      togglingId.value = null
    }
  }

  function openDeleteConfirm(user: AdminUserListVO) {
    if (!canDeleteUser(user)) return
    pendingDeleteUser.value = user
    showDeleteConfirm.value = true
  }

  function closeDeleteConfirm() {
    if (deletingId.value) return
    dismissDeleteConfirm()
  }

  function dismissDeleteConfirm() {
    showDeleteConfirm.value = false
    pendingDeleteUser.value = null
  }

  async function confirmDeleteUser() {
    const user = pendingDeleteUser.value
    if (!user || deletingId.value || !canDeleteUser(user)) return

    deletingId.value = user.id
    errorMsg.value = ''
    try {
      await adminDeleteUser(user.id)
      records.value = records.value.filter((item) => item.id !== user.id)
      total.value = Math.max(0, total.value - 1)
      showToast('用户已删除')
      dismissDeleteConfirm()
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '删除用户失败'
    } finally {
      deletingId.value = null
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
    pageSize,
    total,
    totalPages,
    filterNickname,
    filterMobile,
    filterStatus,
    filterStartTime,
    filterEndTime,
    showEditModal,
    editingUser,
    editNickname,
    editSubmitting,
    editErrorMsg,
    showStatusConfirm,
    pendingStatusUser,
    togglingId,
    showDeleteConfirm,
    pendingDeleteUser,
    deletingId,
    hasRecords,
    hasPrevPage,
    hasNextPage,
    UserStatus,
    loadList,
    handleSearch,
    handleReset,
    goPrevPage,
    goNextPage,
    openEditModal,
    closeEditModal,
    submitEdit,
    openStatusConfirm,
    closeStatusConfirm,
    confirmToggleStatus,
    openDeleteConfirm,
    closeDeleteConfirm,
    confirmDeleteUser,
    canDeleteUser,
    getDeleteDisabledReason,
  }
}
