import { computed, ref } from 'vue'
import { getUserProfile, updateUserProfile, uploadAvatar } from '@/api/user'
import type { UserProfileVO } from '@/types/user'
import { UserStatus } from '@/types/user'
import { getUserInfo, setUserInfo } from '@/utils/auth'
import { resolveFileUrl } from '@/utils/file'

/** 头像文件大小上限：5MB */
export const AVATAR_MAX_SIZE = 5 * 1024 * 1024

export function useUserProfile() {
  const loading = ref(false)
  const saving = ref(false)
  const uploading = ref(false)
  const errorMsg = ref('')
  const successMsg = ref('')

  const profile = ref<UserProfileVO | null>(null)
  const nickname = ref('')
  const originalNickname = ref('')

  const avatarUrl = computed(() => resolveFileUrl(profile.value?.avatar))
  const maskedMobile = computed(() => profile.value?.mobile ?? '')
  const isDisabled = computed(() => profile.value?.status === UserStatus.DISABLED)
  const avatarInitial = computed(() => {
    const name = profile.value?.nickname?.trim() || nickname.value.trim()
    return (name || '用').charAt(0).toUpperCase()
  })

  function applyProfile(data: UserProfileVO) {
    profile.value = data
    nickname.value = data.nickname
    originalNickname.value = data.nickname
  }

  function syncLocalUserInfo(data?: UserProfileVO) {
    const source = data ?? profile.value
    const current = getUserInfo()
    if (!current || !source) return

    setUserInfo({
      ...current,
      id: source.id,
      nickname: source.nickname,
      avatar: source.avatar,
    })
  }

  function clearMessages() {
    errorMsg.value = ''
    successMsg.value = ''
  }

  /** 查看用户资料 GET /user/profile */
  async function loadProfile() {
    loading.value = true
    clearMessages()
    try {
      const data = await getUserProfile()
      applyProfile(data)
      syncLocalUserInfo(data)
      return data
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载资料失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  /** 完善用户资料 POST /user/profile/update */
  async function saveProfile() {
    const nextNickname = nickname.value.trim()
    if (!nextNickname) {
      errorMsg.value = '用户昵称不能为空'
      return false
    }

    if (nextNickname === originalNickname.value) {
      successMsg.value = '资料未变更'
      return true
    }

    clearMessages()

    try {
      saving.value = true
      await updateUserProfile({ nickname: nextNickname })
      await loadProfile()
      successMsg.value = '资料保存成功'
      return true
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '保存失败'
      return false
    } finally {
      saving.value = false
    }
  }

  /** 上传用户头像 POST /user/avatar/upload */
  async function uploadUserAvatar(file: File) {
    if (!file.type.startsWith('image/')) {
      errorMsg.value = '请选择图片文件'
      return false
    }

    if (file.size > AVATAR_MAX_SIZE) {
      errorMsg.value = '图片大小不能超过 5MB'
      return false
    }

    clearMessages()

    try {
      uploading.value = true
      await uploadAvatar(file)
      await loadProfile()
      successMsg.value = '头像更新成功'
      return true
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '头像上传失败'
      return false
    } finally {
      uploading.value = false
    }
  }

  return {
    loading,
    saving,
    uploading,
    errorMsg,
    successMsg,
    profile,
    nickname,
    avatarUrl,
    maskedMobile,
    isDisabled,
    avatarInitial,
    loadProfile,
    saveProfile,
    uploadUserAvatar,
    clearMessages,
  }
}
