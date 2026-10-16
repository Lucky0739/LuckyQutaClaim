import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { applySeckillGoods, getActivityList, listSeckillGoodsApply } from '@/api/activity'
import { getMyGoodsDetail, getMyGoodsPage } from '@/api/goods'
import { formatDateTime } from '@/composables/useAdminUserList'
import type { SeckillActivityVO, SeckillGoodsApplyVO } from '@/types/activity'
import {
  formatActivityStatus,
  formatSeckillGoodsAuditStatus,
  formatSeckillPrice,
  SeckillActivityStatus,
  SeckillGoodsAuditStatus,
} from '@/types/activity'
import { GoodsAuditStatus, GoodsStatus, type GoodsListVO, type GoodsSkuVO } from '@/types/goods'
import { getUserInfo } from '@/utils/auth'

const PAGE_SIZE = 10
const MY_GOODS_PAGE_SIZE = 100

export type MerchantSeckillTab = 'apply' | 'records'

function buildAppliedSkuKey(activityId: string, goodsId: string, skuId: string): string {
  return `${activityId}:${goodsId}:${skuId}`
}

function resolveSeckillImg(sku?: GoodsSkuVO | null, goods?: GoodsListVO | null): string | undefined {
  return sku?.skuImg?.trim() || goods?.goodsImg?.trim() || undefined
}

function getCurrentUserId(): string {
  const id = getUserInfo()?.id
  return id != null ? String(id) : ''
}

function isActiveApplication(item: SeckillGoodsApplyVO): boolean {
  return (
    item.auditStatus === SeckillGoodsAuditStatus.PENDING
    || item.auditStatus === SeckillGoodsAuditStatus.APPROVED
  )
}

function isValidPriceInput(value: string): boolean {
  return /^\d+(\.\d{1,2})?$/.test(value.trim())
}

export function useMerchantSeckillApply() {
  const route = useRoute()
  const activeTab = ref<MerchantSeckillTab>('apply')

  const pageLoading = ref(false)
  const pageError = ref('')
  const toastMsg = ref('')

  const activities = ref<SeckillActivityVO[]>([])
  const eligibleGoods = ref<GoodsListVO[]>([])
  const skuList = ref<GoodsSkuVO[]>([])
  const skuLoading = ref(false)

  const formActivityId = ref('')
  const formGoodsId = ref('')
  const formSkuId = ref('')
  const formSeckillTitle = ref('')
  const formSeckillPrice = ref('')
  const formSeckillTotal = ref('')
  const formSeckillLimit = ref('1')
  const formSort = ref('0')
  const applySubmitting = ref(false)
  const applyErrorMsg = ref('')

  const recordsLoading = ref(false)
  const recordsErrorMsg = ref('')
  const records = ref<SeckillGoodsApplyVO[]>([])
  const pageNum = ref(1)
  const total = ref(0)
  const totalPages = ref(0)
  const filterActivityId = ref('')
  const filterAuditStatus = ref<SeckillGoodsAuditStatus | ''>('')
  const activityApplications = ref<SeckillGoodsApplyVO[]>([])
  const activityApplicationsLoading = ref(false)

  const applyableActivities = computed(() =>
    activities.value.filter((item) => item.status === SeckillActivityStatus.NOT_STARTED),
  )

  const selectedActivity = computed(
    () => applyableActivities.value.find((item) => item.id === formActivityId.value) ?? null,
  )

  const selectedGoods = computed(() =>
    eligibleGoods.value.find((item) => item.id === formGoodsId.value) ?? null,
  )

  const selectedSku = computed(() =>
    skuList.value.find((item) => item.id === formSkuId.value) ?? null,
  )

  const seckillImgPreview = computed(() => resolveSeckillImg(selectedSku.value, selectedGoods.value))

  const appliedSkuKeys = computed(() => {
    const keys = new Set<string>()
    const currentUserId = getCurrentUserId()
    for (const item of activityApplications.value) {
      if (currentUserId && item.userId && item.userId !== currentUserId) continue
      if (!isActiveApplication(item)) continue
      keys.add(buildAppliedSkuKey(item.activityId, item.goodsId, item.skuId))
    }
    return keys
  })

  const availableSkuList = computed(() => {
    if (!formActivityId.value) return skuList.value
    return skuList.value.filter((sku) => {
      if (!sku.id) return false
      return !appliedSkuKeys.value.has(
        buildAppliedSkuKey(formActivityId.value, formGoodsId.value, sku.id),
      )
    })
  })

  const hasRecords = computed(() => records.value.length > 0)
  const hasPrevPage = computed(() => pageNum.value > 1)
  const hasNextPage = computed(() => pageNum.value < totalPages.value)

  const applyFormHint = computed(() => {
    if (!formActivityId.value) return '请先选择秒杀活动'
    if (!formGoodsId.value) return '请先选择要参与秒杀的商品'
    if (skuLoading.value) return '正在加载商品规格...'
    if (!skuList.value.length) return '该商品暂无可用规格，请先在商品管理中配置 SKU'
    if (!availableSkuList.value.length) return '该商品在当前活动下的规格均已申请，请更换商品或活动'
    if (!formSkuId.value) return '请选择商品规格'
    return ''
  })

  function showToast(message: string) {
    toastMsg.value = message
    window.setTimeout(() => {
      if (toastMsg.value === message) {
        toastMsg.value = ''
      }
    }, 2000)
  }

  function formatActivityOption(activity: SeckillActivityVO): string {
    const timeParts: string[] = []
    if (activity.beginTime) timeParts.push(formatDateTime(activity.beginTime))
    if (activity.endTime) timeParts.push(formatDateTime(activity.endTime))
    const timeText = timeParts.length ? ` · ${timeParts.join(' ~ ')}` : ''
    return `${activity.activityName}（${formatActivityStatus(activity.status)}${timeText}）`
  }

  async function applyRoutePreset() {
    const goodsId = route.query.goodsId
    if (typeof goodsId !== 'string' || !goodsId.trim()) return
    const normalizedGoodsId = goodsId.trim()
    if (!eligibleGoods.value.some((item) => item.id === normalizedGoodsId)) return
    formGoodsId.value = normalizedGoodsId
  }

  async function loadPageData() {
    pageLoading.value = true
    pageError.value = ''
    try {
      const [activityList, goodsPage] = await Promise.all([
        getActivityList(),
        getMyGoodsPage({
          pageNum: 1,
          pageSize: MY_GOODS_PAGE_SIZE,
          status: GoodsStatus.ON_SHELF,
          auditStatus: GoodsAuditStatus.PASS,
        }),
      ])
      activities.value = activityList
      eligibleGoods.value = goodsPage.records ?? []

      if (!formActivityId.value && applyableActivities.value.length) {
        formActivityId.value = applyableActivities.value[0]!.id
      }

      if (formActivityId.value) {
        await loadActivityApplications(formActivityId.value)
      }

      await applyRoutePreset()
    } catch (err) {
      pageError.value = err instanceof Error ? err.message : '加载页面数据失败'
      activities.value = []
      eligibleGoods.value = []
    } finally {
      pageLoading.value = false
    }
  }

  async function loadActivityApplications(activityId: string) {
    if (!activityId) {
      activityApplications.value = []
      return
    }

    activityApplicationsLoading.value = true
    try {
      const result = await listSeckillGoodsApply({
        pageNum: 1,
        pageSize: MY_GOODS_PAGE_SIZE,
        activityId,
      })
      activityApplications.value = result.records ?? []
    } catch {
      activityApplications.value = []
    } finally {
      activityApplicationsLoading.value = false
    }
  }

  function syncSelectedSku() {
    if (!availableSkuList.value.length) {
      formSkuId.value = ''
      return
    }
    if (!availableSkuList.value.some((item) => item.id === formSkuId.value)) {
      formSkuId.value = availableSkuList.value[0]?.id ?? ''
    }
  }

  async function loadSkuList(goodsId: string) {
    if (!goodsId) {
      skuList.value = []
      formSkuId.value = ''
      return
    }

    skuLoading.value = true
    applyErrorMsg.value = ''
    try {
      const detail = await getMyGoodsDetail(goodsId)
      skuList.value = detail.skuList?.filter((item) => item.id) ?? []
      if (!skuList.value.length) {
        applyErrorMsg.value = '该商品暂无可用规格，请先在商品管理中配置 SKU'
      }
      syncSelectedSku()
      if (!formSeckillTitle.value.trim()) {
        formSeckillTitle.value = detail.goodsName
      }
    } catch (err) {
      skuList.value = []
      formSkuId.value = ''
      applyErrorMsg.value = err instanceof Error ? err.message : '加载商品规格失败'
    } finally {
      skuLoading.value = false
    }
  }

  function resetApplyForm() {
    formGoodsId.value = ''
    formSkuId.value = ''
    formSeckillTitle.value = ''
    formSeckillPrice.value = ''
    formSeckillTotal.value = ''
    formSeckillLimit.value = '1'
    formSort.value = '0'
    skuList.value = []
    applyErrorMsg.value = ''
  }

  function validateApplyForm(): string | null {
    if (!formActivityId.value) return '请选择秒杀活动'
    if (!formGoodsId.value) return '请选择商品'
    if (!formSkuId.value) return '请选择商品规格'

    if (
      appliedSkuKeys.value.has(
        buildAppliedSkuKey(formActivityId.value, formGoodsId.value, formSkuId.value),
      )
    ) {
      return '该规格已提交过申请，请勿重复提交'
    }

    const priceText = formSeckillPrice.value.trim()
    if (!isValidPriceInput(priceText)) return '秒杀价最多保留两位小数'
    const price = Number(priceText)
    if (!Number.isFinite(price) || price <= 0) return '请输入有效的秒杀价'

    const sku = selectedSku.value
    if (sku?.salePrice != null && price >= sku.salePrice) {
      return `秒杀价须低于 SKU 售价 ¥${formatSeckillPrice(sku.salePrice)}`
    }

    const totalStock = Number(formSeckillTotal.value.trim())
    if (!Number.isInteger(totalStock) || totalStock < 1) return '秒杀库存必须为正整数'
    if (sku?.stockNum != null && totalStock > sku.stockNum) {
      return `秒杀库存不能超过 SKU 可用库存 ${sku.stockNum}`
    }

    const limit = Number(formSeckillLimit.value.trim())
    if (!Number.isInteger(limit) || limit < 1) return '限购数量必须为正整数'
    if (limit > totalStock) return '限购数量不能大于秒杀库存'

    const sort = Number(formSort.value.trim())
    if (!Number.isInteger(sort) || sort < 0) return '排序值必须为非负整数'

    return null
  }

  async function submitApply() {
    if (applySubmitting.value) return

    const validationError = validateApplyForm()
    if (validationError) {
      applyErrorMsg.value = validationError
      showToast(validationError)
      return
    }

    applySubmitting.value = true
    applyErrorMsg.value = ''
    try {
      const seckillImg = resolveSeckillImg(selectedSku.value, selectedGoods.value)
      await applySeckillGoods({
        activityId: formActivityId.value,
        goodsId: formGoodsId.value,
        skuId: formSkuId.value,
        seckillTitle: formSeckillTitle.value.trim() || undefined,
        seckillImg,
        seckillPrice: Number(formSeckillPrice.value.trim()),
        seckillTotal: Number(formSeckillTotal.value.trim()),
        seckillLimit: Number(formSeckillLimit.value.trim()),
        sort: Number(formSort.value.trim()),
      })
      resetApplyForm()
      showToast('秒杀商品申请已提交，请等待审核')
      activeTab.value = 'records'
      pageNum.value = 1
      await Promise.all([
        loadRecords(),
        loadActivityApplications(formActivityId.value),
      ])
    } catch (err) {
      applyErrorMsg.value = err instanceof Error ? err.message : '提交申请失败'
      showToast(applyErrorMsg.value)
    } finally {
      applySubmitting.value = false
    }
  }

  async function loadRecords() {
    recordsLoading.value = true
    recordsErrorMsg.value = ''
    try {
      const result = await listSeckillGoodsApply({
        pageNum: pageNum.value,
        pageSize: PAGE_SIZE,
        activityId: filterActivityId.value || undefined,
        auditStatus: filterAuditStatus.value === '' ? undefined : filterAuditStatus.value,
      })

      records.value = result.records ?? []
      total.value = result.total ?? 0
      totalPages.value = Math.max(result.totalPages ?? 0, records.value.length ? 1 : 0)
      pageNum.value = result.pageNum ?? pageNum.value

      if (totalPages.value > 0 && pageNum.value > totalPages.value) {
        pageNum.value = totalPages.value
      }
    } catch (err) {
      recordsErrorMsg.value = err instanceof Error ? err.message : '加载申请记录失败'
      records.value = []
      total.value = 0
      totalPages.value = 0
    } finally {
      recordsLoading.value = false
    }
  }

  function switchTab(tab: MerchantSeckillTab) {
    activeTab.value = tab
    if (tab === 'records') {
      void loadRecords()
    }
  }

  function handleRecordsSearch() {
    pageNum.value = 1
    void loadRecords()
  }

  function handleRecordsReset() {
    filterActivityId.value = ''
    filterAuditStatus.value = ''
    pageNum.value = 1
    void loadRecords()
  }

  function goPrevPage() {
    if (!hasPrevPage.value || recordsLoading.value) return
    pageNum.value -= 1
    void loadRecords()
  }

  function goNextPage() {
    if (!hasNextPage.value || recordsLoading.value) return
    pageNum.value += 1
    void loadRecords()
  }

  watch(formGoodsId, (goodsId) => {
    formSeckillPrice.value = ''
    formSeckillTotal.value = ''
    void loadSkuList(goodsId)
  })

  watch(formActivityId, (activityId) => {
    syncSelectedSku()
    void loadActivityApplications(activityId)
  })

  onMounted(() => {
    void loadPageData()
  })

  return {
    activeTab,
    pageLoading,
    pageError,
    toastMsg,
    activities,
    applyableActivities,
    eligibleGoods,
    skuList,
    availableSkuList,
    skuLoading,
    activityApplicationsLoading,
    formActivityId,
    formGoodsId,
    formSkuId,
    formSeckillTitle,
    formSeckillPrice,
    formSeckillTotal,
    formSeckillLimit,
    formSort,
    applySubmitting,
    applyErrorMsg,
    applyFormHint,
    selectedActivity,
    selectedGoods,
    selectedSku,
    seckillImgPreview,
    recordsLoading,
    recordsErrorMsg,
    records,
    pageNum,
    total,
    totalPages,
    filterActivityId,
    filterAuditStatus,
    hasRecords,
    hasPrevPage,
    hasNextPage,
    loadPageData,
    submitApply,
    resetApplyForm,
    switchTab,
    handleRecordsSearch,
    handleRecordsReset,
    goPrevPage,
    goNextPage,
    loadRecords,
    formatDateTime,
    formatActivityOption,
    formatSeckillGoodsAuditStatus,
    formatSeckillPrice,
    SeckillGoodsAuditStatus,
  }
}
