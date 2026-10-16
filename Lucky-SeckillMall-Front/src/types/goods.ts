/** 商品 SKU 请求，对应后端 GoodsSkuRequest */
export interface GoodsSkuRequest {
  skuSpecName?: string
  skuImg?: string
  marketPrice?: number
  salePrice?: number
  stockNum?: number
}

/** 更新 SKU，对应后端 UpdateGoodsSkuRequest */
export interface UpdateGoodsSkuRequest {
  skuId?: string
  skuSpecName?: string
  skuImg?: string
  marketPrice?: number
  salePrice?: number
  stockNum?: number
}

/** 商户更新商品请求，对应后端 UpdateGoodsRequest */
export interface UpdateGoodsRequest {
  goodsId: GoodsId
  goodsName?: string
  goodsImg?: string
  goodsPrice?: number
  categoryId?: string
  shopId?: string
  detailContent?: string
  goodsImgList?: string[]
  skuList?: UpdateGoodsSkuRequest[]
}

/** 新增商品请求，对应后端 AddGoodsRequest */
export interface AddGoodsRequest {
  goodsName: string
  goodsPrice: number
  categoryId?: string
  shopId?: string
  goodsImgList?: string[]
  detailContent?: string
  skuList?: GoodsSkuRequest[]
}

export type GoodsId = string

/** 商品状态：0=下架，1=上架 */
export const GoodsStatus = {
  OFF_SHELF: 0,
  ON_SHELF: 1,
} as const
export type GoodsStatus = (typeof GoodsStatus)[keyof typeof GoodsStatus]

/**
 * 商品审核状态（与库表 audit_status、GoodsAuditRequest.AUDIT_PASS/REJECT 一致）
 * 0=未申请 1=待审核 2=通过 3=拒绝
 */
export const GoodsAuditStatus = {
  NOT_APPLIED: 0,
  PENDING: 1,
  PASS: 2,
  REJECT: 3,
} as const
export type GoodsAuditStatus = (typeof GoodsAuditStatus)[keyof typeof GoodsAuditStatus]

/** @deprecated 请使用 GoodsAuditStatus，保留别名便于商户模块引用 */
export const MerchantGoodsAuditStatus = GoodsAuditStatus
export type MerchantGoodsAuditStatus = GoodsAuditStatus

const GOODS_AUDIT_STATUS_LABELS: Record<GoodsAuditStatus, string> = {
  [GoodsAuditStatus.NOT_APPLIED]: '未申请',
  [GoodsAuditStatus.PENDING]: '待审核',
  [GoodsAuditStatus.PASS]: '审核通过',
  [GoodsAuditStatus.REJECT]: '审核拒绝',
}

export function formatMerchantGoodsAuditStatus(status?: number): string {
  return formatGoodsAuditStatus(status)
}

/** 商户是否可申请上架（下架且非待审核） */
export function canMerchantApplyOnShelf(
  item: Pick<GoodsListVO, 'status' | 'auditStatus'>,
): boolean {
  if (item.status === GoodsStatus.ON_SHELF) return false
  if (item.auditStatus === GoodsAuditStatus.PENDING) return false
  return true
}

/** 商户是否可下架 */
export function canMerchantOffShelf(item: Pick<GoodsListVO, 'status'>): boolean {
  return item.status === GoodsStatus.ON_SHELF
}

/** 商品审核请求，对应后端 GoodsAuditRequest（仅提交 2=通过 或 3=拒绝） */
export interface GoodsAuditRequest {
  goodsId: GoodsId
  auditStatus: typeof GoodsAuditStatus.PASS | typeof GoodsAuditStatus.REJECT
  auditRemark?: string
}

export function formatGoodsAuditStatus(status?: number, desc?: string): string {
  if (desc) return desc
  if (status == null) return '--'
  return GOODS_AUDIT_STATUS_LABELS[status as GoodsAuditStatus] ?? `状态${status}`
}

export function isGoodsAuditPending(status?: number): boolean {
  return status === GoodsAuditStatus.PENDING
}

export function canAuditGoods(status?: number): boolean {
  return status === GoodsAuditStatus.PENDING
}

/** 首页/搜索商品分页大小（无限滚动每次加载条数） */
export const HOME_GOODS_PAGE_SIZE = 15

/** 首页排行 sortType，对应后端 GoodsRankSortEnum（hot 等同 weight） */
export type GoodsHomeSortType = 'weight' | 'collect' | 'sales' | 'hot'

export interface GoodsHomeQueryRequest {
  sortType?: GoodsHomeSortType
  pageNum: number
  pageSize: number
}

export const GOODS_HOME_SORT_OPTIONS: { value: GoodsHomeSortType; label: string }[] = [
  { value: 'weight', label: '综合推荐' },
  { value: 'collect', label: '收藏最多' },
  { value: 'sales', label: '销量最高' },
]

export function normalizeHomeSortType(value?: string): GoodsHomeSortType {
  const code = value?.trim().toLowerCase()
  if (code === 'collect' || code === 'sales') return code
  return 'weight'
}

export function getHomeSortLabel(sortType: GoodsHomeSortType): string {
  return GOODS_HOME_SORT_OPTIONS.find((item) => item.value === sortType)?.label ?? '综合推荐'
}

/** 商品列表查询请求，对应后端 GoodsQueryRequest */
export interface GoodsQueryRequest {
  goodsName?: string
  status?: GoodsStatus
  auditStatus?: number
  /** 子分类 ID（叶子分类，精确匹配） */
  categoryId?: string
  /** 父分类 ID（查询该父分类下所有子分类商品） */
  parentCategoryId?: string
  shopId?: string
  startTime?: string
  endTime?: string
  pageNum: number
  pageSize: number
}

/** 首页/搜索按分类筛选（展示名仅前端用） */
export interface GoodsCategorySelection {
  parentCategoryId?: string
  categoryId?: string
  parentName?: string
  categoryName?: string
}

export function isCategoryFilterActive(filter?: GoodsCategorySelection | null): boolean {
  return !!(filter?.categoryId || filter?.parentCategoryId)
}

/** 商品列表项，对应后端 GoodsListVO */
export interface GoodsListVO {
  id: GoodsId
  goodsName: string
  goodsImg?: string
  goodsPrice?: number
  status?: GoodsStatus
  statusDesc?: string
  auditStatus?: number
  auditStatusDesc?: string
  /** 已售数量（所有 SKU 销量之和） */
  salesNum?: number
  categoryId?: string
  categoryName?: string
  parentCategoryId?: string
  parentCategoryName?: string
  createTime?: string
  updateTime?: string
}

/** 商品 SKU 展示项，兼容后端 GoodsSku / 详情 VO 返回 */
export interface GoodsSkuVO {
  id?: string
  skuSpecName?: string
  skuImg?: string
  marketPrice?: number
  salePrice?: number
  stockNum?: number
  salesNum?: number
}

/** 规格图片，对应后端 SkuImgVO */
export interface SkuImgVO {
  skuId?: string
  skuSpecName?: string
  skuImg?: string
}

/** 商品详情，对应后端 GoodsDetailVO，字段按接口返回可选兼容 */
export interface GoodsDetailVO extends GoodsListVO {
  shopId?: string
  shopName?: string
  categoryId?: string
  categoryName?: string
  detailContent?: string
  goodsImgList?: string[]
  skuList?: GoodsSkuVO[]
}

/** 商品收藏请求 */
export interface GoodsCollectRequest {
  goodsId: GoodsId
}

/** 商品收藏状态（对应后端 GoodsCollectStatusVO） */
export interface GoodsCollectStatusVO {
  goodsId?: GoodsId
  /** 商品总收藏人数 */
  collectNum?: number
  /** @deprecated 与 collectNum 同义，兼容旧字段 */
  collectCount?: number
  collected?: boolean
  isCollected?: boolean
}

/** 我的商品收藏列表项（对应后端 GoodsCollectVO） */
export interface GoodsCollectVO extends GoodsListVO {
  goodsId?: GoodsId
  collectNum?: number
  /** 收藏时间，后端字段为 createTime */
  collectTime?: string
  createTime?: string
}

export type GoodsListRaw = Omit<GoodsListVO, 'id' | 'goodsPrice' | 'categoryId' | 'parentCategoryId'> & {
  id?: string | number
  goodsId?: string | number
  goodsPrice?: number | string
  categoryId?: string | number
  parentCategoryId?: string | number
}

export type GoodsSkuRaw = Omit<GoodsSkuVO, 'id' | 'marketPrice' | 'salePrice'> & {
  id?: string | number
  skuId?: string | number
  marketPrice?: number | string
  salePrice?: number | string
}

export type SkuImgRaw = Omit<SkuImgVO, 'skuId'> & {
  skuId?: string | number
}

export type GoodsDetailRaw = Omit<
  GoodsDetailVO,
  'id' | 'goodsPrice' | 'shopId' | 'categoryId' | 'skuList'
> & {
  id?: string | number
  goodsId?: string | number
  goodsPrice?: number | string
  shopId?: string | number
  categoryId?: string | number
  skuList?: GoodsSkuRaw[]
}

export type GoodsCollectStatusRaw = Omit<GoodsCollectStatusVO, 'goodsId'> & {
  goodsId?: string | number
  collectNum?: number
}

export type GoodsCollectRaw = Omit<GoodsCollectVO, 'id' | 'goodsId' | 'goodsPrice'> & {
  id?: string | number
  goodsId?: string | number
  goodsPrice?: number | string
  collectNum?: number
  createTime?: string
}

function toIdString(value?: string | number | null): string {
  if (value === undefined || value === null || value === '') return ''
  return String(value)
}

export function normalizeGoodsListItem(raw: GoodsListRaw): GoodsListVO {
  const price =
    raw.goodsPrice == null || raw.goodsPrice === ''
      ? undefined
      : Number(raw.goodsPrice)

  return {
    id: toIdString(raw.id ?? raw.goodsId),
    goodsName: raw.goodsName,
    goodsImg: raw.goodsImg,
    goodsPrice: Number.isFinite(price) ? price : undefined,
    status: raw.status,
    statusDesc: raw.statusDesc,
    auditStatus: raw.auditStatus,
    auditStatusDesc: raw.auditStatusDesc,
    salesNum: raw.salesNum ?? 0,
    categoryId: toIdString(raw.categoryId),
    categoryName: raw.categoryName,
    parentCategoryId: toIdString(raw.parentCategoryId),
    parentCategoryName: raw.parentCategoryName,
    createTime: raw.createTime,
    updateTime: raw.updateTime,
  }
}

export function normalizeGoodsList(records: GoodsListRaw[]): GoodsListVO[] {
  if (!Array.isArray(records)) return []
  return records.map(normalizeGoodsListItem).filter((item) => item.id)
}

function toNumber(value?: number | string | null): number | undefined {
  if (value === undefined || value === null || value === '') return undefined
  const num = Number(value)
  return Number.isFinite(num) ? num : undefined
}

export function normalizeGoodsSku(raw: GoodsSkuRaw): GoodsSkuVO {
  return {
    ...raw,
    id: toIdString(raw.id ?? raw.skuId),
    marketPrice: toNumber(raw.marketPrice),
    salePrice: toNumber(raw.salePrice),
    stockNum: raw.stockNum ?? 0,
    salesNum: raw.salesNum ?? 0,
  }
}

export function normalizeSkuImg(raw: SkuImgRaw): SkuImgVO {
  return {
    skuId: toIdString(raw.skuId),
    skuSpecName: raw.skuSpecName,
    skuImg: raw.skuImg,
  }
}

export function normalizeGoodsDetail(raw: GoodsDetailRaw): GoodsDetailVO {
  const base = normalizeGoodsListItem({
    ...raw,
    id: raw.id ?? raw.goodsId,
  })
  const goodsImgList = Array.isArray(raw.goodsImgList)
    ? raw.goodsImgList.filter(Boolean)
    : []
  return {
    ...base,
    shopId: toIdString(raw.shopId),
    shopName: raw.shopName,
    categoryId: toIdString(raw.categoryId),
    categoryName: raw.categoryName,
    detailContent: raw.detailContent,
    goodsImgList,
    skuList: Array.isArray(raw.skuList) ? raw.skuList.map(normalizeGoodsSku) : [],
  }
}

export function normalizeGoodsCollectStatus(raw: GoodsCollectStatusRaw): GoodsCollectStatusVO {
  const collectNum = raw.collectNum ?? raw.collectCount ?? 0
  return {
    ...raw,
    goodsId: toIdString(raw.goodsId),
    collectNum,
    collectCount: collectNum,
    collected: raw.collected ?? raw.isCollected ?? false,
    isCollected: raw.isCollected ?? raw.collected ?? false,
  }
}

export function normalizeGoodsCollectItem(raw: GoodsCollectRaw): GoodsCollectVO {
  const item = normalizeGoodsListItem({
    ...raw,
    id: raw.goodsId ?? raw.id,
  })
  return {
    ...item,
    goodsId: toIdString(raw.goodsId ?? raw.id),
    collectNum: raw.collectNum,
    collectTime: raw.collectTime ?? raw.createTime,
    createTime: raw.createTime,
  }
}

export function normalizeGoodsCollectList(records: GoodsCollectRaw[]): GoodsCollectVO[] {
  if (!Array.isArray(records)) return []
  return records.map(normalizeGoodsCollectItem).filter((item) => item.id)
}

export function formatGoodsPrice(price?: number | null): string {
  if (price == null || !Number.isFinite(price)) return '--'
  return price.toFixed(2)
}

/** 格式化商品分类展示 */
export function formatGoodsCategory(
  item: Pick<GoodsListVO, 'parentCategoryName' | 'categoryName'>,
): string {
  const parts = [item.parentCategoryName, item.categoryName].filter(Boolean)
  return parts.length ? parts.join(' / ') : '--'
}

/** 格式化已售数量展示 */
export function formatGoodsSalesNum(salesNum?: number | null): string {
  const count = salesNum ?? 0
  if (count <= 0) return '已售0件'
  if (count >= 10000) {
    return `已售${(count / 10000).toFixed(1)}万件`
  }
  return `已售${count}件`
}

/** 首页排行列表查询参数 GET /goods/home/list */
export function buildHomeGoodsListQuery(
  pageNum: number,
  sortType: GoodsHomeSortType = 'weight',
): GoodsHomeQueryRequest {
  return {
    sortType: normalizeHomeSortType(sortType),
    pageNum,
    pageSize: HOME_GOODS_PAGE_SIZE,
  }
}

/** 分类/搜索等场景：分页查询上架商品 GET /goods/page */
export function buildHomeGoodsQuery(pageNum: number, pageSize = HOME_GOODS_PAGE_SIZE): GoodsQueryRequest {
  return {
    pageNum,
    pageSize,
    status: GoodsStatus.ON_SHELF,
  }
}

/** 首页排行接口无数据时回退：与 Redis 排行可见范围一致（上架且审核通过） */
export function buildHomeGoodsRankFallbackQuery(
  pageNum: number,
  pageSize = HOME_GOODS_PAGE_SIZE,
): GoodsQueryRequest {
  return {
    ...buildHomeGoodsQuery(pageNum, pageSize),
    auditStatus: GoodsAuditStatus.PASS,
  }
}

/** 搜索上架商品查询参数 */
export function buildGoodsSearchQuery(pageNum: number, goodsName: string): GoodsQueryRequest {
  const keyword = goodsName.trim()
  return {
    pageNum,
    pageSize: HOME_GOODS_PAGE_SIZE,
    status: GoodsStatus.ON_SHELF,
    ...(keyword ? { goodsName: keyword } : {}),
  }
}

/** 按父/子分类查询上架商品（GET /goods/page） */
export function buildCategoryGoodsQuery(
  pageNum: number,
  filter: GoodsCategorySelection,
): GoodsQueryRequest {
  const query: GoodsQueryRequest = {
    pageNum,
    pageSize: HOME_GOODS_PAGE_SIZE,
    status: GoodsStatus.ON_SHELF,
  }
  if (filter.categoryId) {
    query.categoryId = filter.categoryId
    if (filter.parentCategoryId) {
      query.parentCategoryId = filter.parentCategoryId
    }
  } else if (filter.parentCategoryId) {
    query.parentCategoryId = filter.parentCategoryId
  }
  return query
}

/** 分类筛选展示标题 */
export function getCategoryFeedTitle(filter?: GoodsCategorySelection | null): string {
  if (!filter) return '全部商品'
  if (filter.categoryName) return filter.categoryName
  if (filter.parentName) return filter.parentName
  return '分类商品'
}

/** 店铺详情页商品查询参数（GET /goods/page?shopId=） */
export interface ShopGoodsQueryParams {
  pageNum: number
  pageSize: number
  status?: GoodsStatus
  auditStatus?: number
}

export function buildShopGoodsQuery(pageNum: number): ShopGoodsQueryParams {
  return {
    pageNum,
    pageSize: HOME_GOODS_PAGE_SIZE,
    status: GoodsStatus.ON_SHELF,
    auditStatus: GoodsAuditStatus.PASS,
  }
}

/** 表单中的 SKU 行 */
export interface GoodsSkuFormItem {
  key: string
  skuSpecName: string
  skuImg: string
  marketPrice: string
  salePrice: string
  stockNum: string
}

export const GOODS_NAME_MAX_LENGTH = 200
export const GOODS_IMG_MAX_SIZE = 5 * 1024 * 1024
export const GOODS_IMG_MAX_COUNT = 5
export const GOODS_DETAIL_MAX_LENGTH = 5000

export function createEmptySkuFormItem(): GoodsSkuFormItem {
  return {
    key: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    skuSpecName: '',
    skuImg: '',
    marketPrice: '',
    salePrice: '',
    stockNum: '',
  }
}

function parsePrice(value: string, label: string): number | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  const num = Number(trimmed)
  if (!Number.isFinite(num) || num < 0) {
    throw new Error(`${label}格式不正确`)
  }
  return num
}

function parseStock(value: string): number | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  const num = Number(trimmed)
  if (!Number.isInteger(num) || num < 0) {
    throw new Error('库存必须为非负整数')
  }
  return num
}

export function buildAddGoodsRequest(form: {
  goodsName: string
  goodsPrice: string
  categoryId?: string
  shopId?: string
  goodsImgList: string[]
  detailContent?: string
  skuList: GoodsSkuFormItem[]
}): AddGoodsRequest {
  const goodsPrice = parsePrice(form.goodsPrice, '商品价格')
  if (goodsPrice === null) {
    throw new Error('商品价格不能为空')
  }

  const payload: AddGoodsRequest = {
    goodsName: form.goodsName.trim(),
    goodsPrice,
  }

  const categoryId = form.categoryId?.trim()
  if (categoryId) payload.categoryId = categoryId

  const shopId = form.shopId?.trim()
  if (shopId) payload.shopId = shopId

  const images = form.goodsImgList.map((url) => url.trim()).filter(Boolean)
  if (images.length) payload.goodsImgList = images

  const detail = form.detailContent?.trim()
  if (detail) payload.detailContent = detail

  const skuList: GoodsSkuRequest[] = []
  for (const [index, sku] of form.skuList.entries()) {
    const specName = sku.skuSpecName.trim()
    const skuImg = sku.skuImg.trim()
    const marketPrice = parsePrice(sku.marketPrice, `SKU 第 ${index + 1} 行市场价`)
    const salePrice = parsePrice(sku.salePrice, `SKU 第 ${index + 1} 行售价`)
    const stockNum = parseStock(sku.stockNum)

    const hasContent =
      specName || skuImg || marketPrice !== null || salePrice !== null || stockNum !== null
    if (!hasContent) continue

    skuList.push({
      skuSpecName: specName || undefined,
      skuImg: skuImg || undefined,
      marketPrice: marketPrice ?? undefined,
      salePrice: salePrice ?? undefined,
      stockNum: stockNum ?? undefined,
    })
  }

  if (skuList.length) payload.skuList = skuList

  return payload
}

export function validatePublishGoodsForm(form: {
  goodsName: string
  goodsPrice: string
  shopId?: string
  goodsImgList: string[]
  detailContent?: string
  skuList: GoodsSkuFormItem[]
}): string | null {
  const goodsName = form.goodsName.trim()
  if (!goodsName) return '商品名称不能为空'
  if (goodsName.length > GOODS_NAME_MAX_LENGTH) {
    return `商品名称不能超过 ${GOODS_NAME_MAX_LENGTH} 个字符`
  }

  try {
    const goodsPrice = parsePrice(form.goodsPrice, '商品价格')
    if (goodsPrice === null) return '商品价格不能为空'
    if (goodsPrice <= 0) return '商品价格必须大于 0'
  } catch (err) {
    return err instanceof Error ? err.message : '商品价格格式不正确'
  }

  if (!form.shopId?.trim()) return '请选择所属店铺'

  const hasMainImages = form.goodsImgList.some((url) => url.trim())
  const hasSkuImage = form.skuList.some((sku) => sku.skuImg.trim())
  if (!hasMainImages && !hasSkuImage) {
    return '请至少上传一张商品主图或 SKU 图片'
  }

  const detail = form.detailContent?.trim() ?? ''
  if (detail.length > GOODS_DETAIL_MAX_LENGTH) {
    return `商品详情不能超过 ${GOODS_DETAIL_MAX_LENGTH} 字`
  }

  for (const [index, sku] of form.skuList.entries()) {
    const specName = sku.skuSpecName.trim()
    const skuImg = sku.skuImg.trim()
    const hasPrice = sku.marketPrice.trim() || sku.salePrice.trim()
    const hasStock = sku.stockNum.trim()

    const hasPartial =
      specName || skuImg || hasPrice || hasStock
    if (!hasPartial) continue

    try {
      if (sku.marketPrice.trim()) parsePrice(sku.marketPrice, `SKU 第 ${index + 1} 行市场价`)
      if (sku.salePrice.trim()) parsePrice(sku.salePrice, `SKU 第 ${index + 1} 行售价`)
      if (hasStock) parseStock(sku.stockNum)
    } catch (err) {
      return err instanceof Error ? err.message : 'SKU 信息填写有误'
    }
  }

  return null
}

export function validateGoodsImageFile(file: File): string | null {
  if (!file.type.startsWith('image/')) {
    return '请选择图片文件'
  }
  if (file.size > GOODS_IMG_MAX_SIZE) {
    return '图片大小不能超过 5MB'
  }
  return null
}
