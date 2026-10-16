import { computed, onMounted, ref } from 'vue'
import { getBannerList, saveBannerImgUrls, uploadBannerImg } from '@/api/banner'
import type { BannerVO } from '@/types/banner'
import { buildBannerSaveRequests, normalizeBannerUrlList } from '@/types/banner'

const ACCEPT_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const MAX_IMAGE_SIZE = 5 * 1024 * 1024

function validateImageFile(file: File): string | null {
  if (!ACCEPT_IMAGE_TYPES.includes(file.type)) {
    return '仅支持 JPG、PNG、WebP、GIF 格式图片'
  }
  if (file.size > MAX_IMAGE_SIZE) {
    return '图片大小不能超过 5MB'
  }
  return null
}

export function useAdminBannerList() {
  const loading = ref(false)
  const errorMsg = ref('')
  const toastMsg = ref('')
  const banners = ref<BannerVO[]>([])
  const uploading = ref(false)
  const saving = ref(false)
  const deletingUrl = ref<string | null>(null)
  const sorting = ref(false)

  const showDeleteConfirm = ref(false)
  const pendingDeleteBanner = ref<BannerVO | null>(null)
  const deleteErrorMsg = ref('')

  const hasBanners = computed(() => banners.value.length > 0)
  const isBusy = computed(
    () => loading.value || uploading.value || saving.value || sorting.value || !!deletingUrl.value,
  )

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
      const list = await getBannerList()
      banners.value = normalizeBannerUrlList(list)
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '加载背景图列表失败'
      banners.value = []
    } finally {
      loading.value = false
    }
  }

  async function persistBanners(nextBanners: BannerVO[]) {
    saving.value = true
    try {
      await saveBannerImgUrls(buildBannerSaveRequests(nextBanners))
      await loadList()
    } finally {
      saving.value = false
    }
  }

  async function uploadAndAdd(file: File) {
    if (uploading.value || saving.value) return

    const validationError = validateImageFile(file)
    if (validationError) {
      showToast(validationError)
      return
    }

    uploading.value = true
    errorMsg.value = ''
    try {
      const result = await uploadBannerImg(file)
      const imageUrl = result.imageUrl?.trim()
      if (!imageUrl) {
        throw new Error('上传成功但未返回图片地址')
      }

      const nextBanners: BannerVO[] = [
        ...banners.value,
        { id: imageUrl, bannerUrl: imageUrl, sort: banners.value.length },
      ]
      await persistBanners(nextBanners)
      showToast('背景图已添加')
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '上传背景图失败'
    } finally {
      uploading.value = false
    }
  }

  function openDeleteConfirm(banner: BannerVO) {
    if (isBusy.value) return
    pendingDeleteBanner.value = banner
    deleteErrorMsg.value = ''
    showDeleteConfirm.value = true
  }

  function closeDeleteConfirm() {
    if (deletingUrl.value) return
    dismissDeleteConfirm()
  }

  function dismissDeleteConfirm() {
    showDeleteConfirm.value = false
    pendingDeleteBanner.value = null
    deleteErrorMsg.value = ''
  }

  async function confirmDelete() {
    const banner = pendingDeleteBanner.value
    if (!banner || deletingUrl.value) return

    deletingUrl.value = banner.bannerUrl
    deleteErrorMsg.value = ''
    try {
      const nextBanners = banners.value.filter((item) => item.bannerUrl !== banner.bannerUrl)
      await persistBanners(nextBanners)
      dismissDeleteConfirm()
      showToast('背景图已删除')
    } catch (err) {
      deleteErrorMsg.value = err instanceof Error ? err.message : '删除背景图失败'
    } finally {
      deletingUrl.value = null
    }
  }

  async function moveBanner(index: number, direction: -1 | 1) {
    if (sorting.value || isBusy.value) return

    const targetIndex = index + direction
    if (targetIndex < 0 || targetIndex >= banners.value.length) return

    const nextBanners = [...banners.value]
    const current = nextBanners[index]
    const target = nextBanners[targetIndex]
    if (!current || !target) return

    nextBanners[index] = target
    nextBanners[targetIndex] = current

    sorting.value = true
    errorMsg.value = ''
    try {
      await persistBanners(nextBanners)
      showToast('排序已更新')
    } catch (err) {
      errorMsg.value = err instanceof Error ? err.message : '更新排序失败'
    } finally {
      sorting.value = false
    }
  }

  function moveUp(index: number) {
    void moveBanner(index, -1)
  }

  function moveDown(index: number) {
    void moveBanner(index, 1)
  }

  onMounted(() => {
    void loadList()
  })

  return {
    loading,
    errorMsg,
    toastMsg,
    banners,
    uploading,
    saving,
    deletingUrl,
    sorting,
    showDeleteConfirm,
    pendingDeleteBanner,
    deleteErrorMsg,
    hasBanners,
    isBusy,
    loadList,
    uploadAndAdd,
    openDeleteConfirm,
    closeDeleteConfirm,
    confirmDelete,
    moveUp,
    moveDown,
  }
}
