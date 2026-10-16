import { computed, ref } from 'vue'
import { addAddress, deleteAddress, getAddressById, listAddress, updateAddress } from '@/api/address'
import {
  AddressDefault,
  buildAddressPayload,
  buildAddressUpdatePayload,
  cloneAddressForm,
  createEmptyAddressForm,
  formatFullAddress,
  isDefaultAddress,
  isSameAddressForm,
  maskAddressMobile,
  normalizeAddressList,
  type AddressFormErrors,
  type AddressFormModel,
  type AddressId,
  type UserAddressVO,
} from '@/types/address'

export function useUserAddress() {
  const loading = ref(false)
  const submitting = ref(false)
  const deletingId = ref<AddressId | null>(null)
  const showModal = ref(false)
  const showDeleteModal = ref(false)
  const pendingDeleteAddress = ref<UserAddressVO | null>(null)
  const editingId = ref<AddressId | null>(null)
  const originalForm = ref<AddressFormModel | null>(null)

  const errorMsg = ref('')
  const successMsg = ref('')
  const toastMsg = ref('')
  const modalErrorMsg = ref('')
  const formErrors = ref<AddressFormErrors>({})

  let toastTimer: ReturnType<typeof setTimeout> | null = null

  const addresses = ref<UserAddressVO[]>([])
  const form = ref<AddressFormModel>(createEmptyAddressForm())

  const isEditing = computed(() => editingId.value !== null)
  const modalTitle = computed(() => (isEditing.value ? '编辑收货地址' : '添加收货地址'))
  const hasAddresses = computed(() => addresses.value.length > 0)
  const addressCount = computed(() => addresses.value.length)
  const defaultAddress = computed(() => addresses.value.find(isDefaultAddress) ?? null)

  function clearMessages() {
    errorMsg.value = ''
    successMsg.value = ''
  }

  function clearToast() {
    if (toastTimer) {
      clearTimeout(toastTimer)
      toastTimer = null
    }
    toastMsg.value = ''
  }

  /** 成功提示弹窗，默认 3 秒后自动关闭 */
  function showSuccessToast(message: string, duration = 3000) {
    clearToast()
    toastMsg.value = message
    toastTimer = setTimeout(() => {
      toastMsg.value = ''
      toastTimer = null
    }, duration)
  }

  function clearModalMessages() {
    modalErrorMsg.value = ''
    formErrors.value = {}
  }

  function resetModalState() {
    editingId.value = null
    originalForm.value = null
    form.value = createEmptyAddressForm()
  }

  async function fetchAddressList() {
    const list = await listAddress()
    addresses.value = normalizeAddressList(list)
    return addresses.value
  }

  /** 查看地址列表 GET /user/address/list */
  async function loadAddresses() {
    loading.value = true
    clearMessages()
    try {
      await fetchAddressList()
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载地址列表失败'
    } finally {
      loading.value = false
    }
  }

  /** 静默刷新列表，保留页面提示信息 */
  async function refreshAddressList() {
    try {
      await fetchAddressList()
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '刷新地址列表失败'
    }
  }

  function openCreate() {
    resetModalState()
    if (!hasAddresses.value) {
      form.value.isDefault = AddressDefault.YES
    }
    clearModalMessages()
    showModal.value = true
  }

  function openEdit(item: UserAddressVO) {
    editingId.value = item.id
    form.value = {
      receiver: item.receiver,
      mobile: item.mobile,
      province: item.province,
      city: item.city,
      area: item.area,
      detailAddress: item.detailAddress,
      isDefault: item.isDefault,
    }
    originalForm.value = cloneAddressForm(form.value)
    clearModalMessages()
    showModal.value = true
  }

  async function openEditById(id: AddressId) {
    clearMessages()
    clearModalMessages()
    try {
      const address = await getAddressById(id)
      openEdit(address)
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载地址详情失败'
    }
  }

  function closeModal() {
    showModal.value = false
    clearModalMessages()
  }

  async function submitAddress() {
    clearModalMessages()

    if (isEditing.value) {
      if (originalForm.value && isSameAddressForm(form.value, originalForm.value)) {
        successMsg.value = '地址未变更'
        closeModal()
        return true
      }

      const { payload, errors } = buildAddressUpdatePayload(form.value, editingId.value!)
      if (!payload) {
        formErrors.value = errors
        modalErrorMsg.value = '请完善地址信息'
        return false
      }

      try {
        submitting.value = true
        await updateAddress(payload)
        closeModal()
        await refreshAddressList()
        successMsg.value = '地址修改成功'
        return true
      } catch (err) {
        modalErrorMsg.value = err instanceof Error ? err.message : '修改失败'
        return false
      } finally {
        submitting.value = false
      }
    }

    const { payload, errors } = buildAddressPayload(form.value)
    if (!payload) {
      formErrors.value = errors
      modalErrorMsg.value = '请完善地址信息'
      return false
    }

    try {
      submitting.value = true
      await addAddress(payload)
      closeModal()
      await refreshAddressList()
      showSuccessToast('地址新增成功')
      return true
    } catch (err) {
      modalErrorMsg.value = err instanceof Error ? err.message : '新增失败'
      return false
    } finally {
      submitting.value = false
    }
  }

  async function setDefaultAddress(item: UserAddressVO) {
    if (isDefaultAddress(item)) return

    clearMessages()
    try {
      await updateAddress({
        id: item.id,
        receiver: item.receiver,
        mobile: item.mobile,
        province: item.province,
        city: item.city,
        area: item.area,
        detailAddress: item.detailAddress,
        isDefault: AddressDefault.YES,
      })
      await refreshAddressList()
      successMsg.value = '已设为默认地址'
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '设置默认地址失败'
    }
  }

  /** 打开删除确认弹窗 */
  function openDeleteConfirm(item: UserAddressVO) {
    pendingDeleteAddress.value = item
    showDeleteModal.value = true
  }

  function closeDeleteConfirm() {
    if (deletingId.value) return
    showDeleteModal.value = false
    pendingDeleteAddress.value = null
  }

  /** 确认删除收货地址 */
  async function confirmDelete() {
    const item = pendingDeleteAddress.value
    if (!item) return

    const addressId = item.id
    clearMessages()
    deletingId.value = addressId

    if (showModal.value && editingId.value === addressId) {
      closeModal()
      resetModalState()
    }

    try {
      await deleteAddress(addressId)
      showDeleteModal.value = false
      pendingDeleteAddress.value = null
      await refreshAddressList()
      successMsg.value = '地址已删除'
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '删除失败'
    } finally {
      deletingId.value = null
    }
  }

  return {
    loading,
    submitting,
    deletingId,
    showModal,
    showDeleteModal,
    pendingDeleteAddress,
    editingId,
    isEditing,
    modalTitle,
    hasAddresses,
    addressCount,
    defaultAddress,
    errorMsg,
    successMsg,
    toastMsg,
    modalErrorMsg,
    formErrors,
    addresses,
    form,
    loadAddresses,
    refreshAddressList,
    openCreate,
    openEdit,
    openEditById,
    closeModal,
    submitAddress,
    setDefaultAddress,
    openDeleteConfirm,
    closeDeleteConfirm,
    confirmDelete,
    formatFullAddress,
    maskAddressMobile,
    isDefaultAddress,
  }
}
