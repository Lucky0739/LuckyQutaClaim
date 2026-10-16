/** 开店请求，对应后端 CreateShopRequest */
import { findDuplicateIds } from '@/utils/json'

export interface CreateShopRequest {
  shopName: string
  shopAvatar?: string
  shopDesc?: string
}

/** 审核状态：0=待审核，1=审核通过，2=审核拒绝 */
export const ShopAuditStatus = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
} as const
export type ShopAuditStatus = (typeof ShopAuditStatus)[keyof typeof ShopAuditStatus]

/** 店铺状态：0=禁用，1=启用 */
export const ShopStatus = {
  DISABLED: 0,
  ENABLED: 1,
} as const
export type ShopStatus = (typeof ShopStatus)[keyof typeof ShopStatus]

export const SHOP_STATUS_LABELS: Record<ShopStatus, string> = {
  [ShopStatus.DISABLED]: '禁用',
  [ShopStatus.ENABLED]: '启用',
}

/** 管理员更新店铺状态请求参数（接口参数名 shopId） */
export interface UpdateShopStatusParams {
  shopId: string
  shopStatus: ShopStatus
}

/** 管理员查询店铺/申请列表请求（pageNum、pageSize 必填） */
export interface ShopApplyQueryRequest {
  pageNum: number
  pageSize: number
  username?: string
  shopName?: string
  auditStatus?: ShopAuditStatus
  shopStatus?: ShopStatus
  startTime?: string
  endTime?: string
}

export const SHOP_QUERY_DEFAULT_PAGE_NUM = 1
export const SHOP_QUERY_DEFAULT_PAGE_SIZE = 10

/** 首页/搜索店铺分页大小 */
export const HOME_SHOP_PAGE_SIZE = 15

/** 公开店铺列表项，对应后端 ShopListVO */
export interface ShopListVO {
  id: string
  shopName: string
  shopAvatar?: string
  shopDesc?: string
  shopStatus?: ShopStatus
}

export type ShopListRaw = Omit<ShopListVO, 'id'> & { id?: string | number }

/** 组装店铺分页查询请求体，始终携带 pageNum、pageSize */
export type ShopApplyQueryInput = {
  pageNum: number
  pageSize: number
  username?: string
  shopName?: string
  auditStatus?: ShopAuditStatus | ''
  shopStatus?: ShopStatus | ''
  startTime?: string
  endTime?: string
}

export function buildShopApplyQueryRequest(params: ShopApplyQueryInput): ShopApplyQueryRequest {
  const body: ShopApplyQueryRequest = {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  }

  const username = params.username?.trim()
  if (username) body.username = username

  const shopName = params.shopName?.trim()
  if (shopName) body.shopName = shopName

  if (params.auditStatus !== '' && params.auditStatus !== undefined) {
    body.auditStatus = params.auditStatus
  }

  if (params.shopStatus !== '' && params.shopStatus !== undefined) {
    body.shopStatus = params.shopStatus
  }

  const startTime = params.startTime?.trim()
  if (startTime) body.startTime = startTime

  const endTime = params.endTime?.trim()
  if (endTime) body.endTime = endTime

  return body
}

/** 管理员审批店铺申请请求 */
export interface ShopAuditRequest {
  shopId: string
  auditStatus: typeof ShopAuditStatus.APPROVED | typeof ShopAuditStatus.REJECTED
  auditRemark?: string
}

/** 店铺列表项（与后端 ShopApplyVO 对齐，id 为 string） */
export interface ShopApplyVO {
  id: string
  userId?: string
  username?: string
  shopName: string
  shopAvatar?: string
  shopDesc?: string
  auditStatus: ShopAuditStatus
  auditStatusDesc?: string
  shopStatus?: ShopStatus
  shopStatusDesc?: string
  auditRemark?: string
  createTime?: string
  updateTime?: string
}

/** JSON 解析后的原始项（id 已在 parse 阶段转为 string） */
export type ShopApplyRaw = Omit<ShopApplyVO, 'id' | 'userId'> & {
  id?: string | number
  userId?: string | number
}

function toIdString(value?: string | number | null): string {
  if (value === undefined || value === null || value === '') return ''
  return String(value)
}

/** 规范化店铺列表项，确保 id / userId 为 string */
export function normalizeShopApplyItem(raw: ShopApplyRaw): ShopApplyVO {
  return {
    id: toIdString(raw.id),
    userId: raw.userId != null ? toIdString(raw.userId) : undefined,
    username: raw.username,
    shopName: raw.shopName,
    shopAvatar: raw.shopAvatar,
    shopDesc: raw.shopDesc,
    auditStatus: raw.auditStatus,
    auditStatusDesc: raw.auditStatusDesc,
    shopStatus: raw.shopStatus,
    shopStatusDesc: raw.shopStatusDesc,
    auditRemark: raw.auditRemark,
    createTime: raw.createTime,
    updateTime: raw.updateTime,
  }
}

export function normalizeShopListItem(raw: ShopListRaw): ShopListVO {
  return {
    id: toIdString(raw.id),
    shopName: raw.shopName,
    shopAvatar: raw.shopAvatar,
    shopDesc: raw.shopDesc,
    shopStatus: raw.shopStatus,
  }
}

export function normalizeShopList(records: ShopListRaw[]): ShopListVO[] {
  return records.map(normalizeShopListItem)
}

export type ShopId = string

/** 店铺收藏请求，对应后端 ShopCollectRequest */
export interface ShopCollectRequest {
  shopId: ShopId
}

/** 店铺收藏状态，对应后端 ShopCollectStatusVO */
export interface ShopCollectStatusVO {
  shopId?: ShopId
  collectNum?: number
  /** @deprecated 与 collectNum 同义 */
  collectCount?: number
  collected?: boolean
  isCollected?: boolean
}

/** 我的店铺收藏列表项，对应后端 ShopCollectVO */
export interface ShopCollectVO {
  id?: ShopId
  shopId: ShopId
  shopName: string
  shopAvatar?: string
  shopDesc?: string
  collectNum?: number
  /** 收藏时间，后端字段为 createTime */
  collectTime?: string
  createTime?: string
}

export type ShopCollectStatusRaw = Omit<ShopCollectStatusVO, 'shopId'> & {
  shopId?: string | number
  collectNum?: number
}

export type ShopCollectRaw = Omit<ShopCollectVO, 'id' | 'shopId'> & {
  id?: string | number
  shopId?: string | number
  collectNum?: number
  createTime?: string
}

export function normalizeShopCollectStatus(raw: ShopCollectStatusRaw): ShopCollectStatusVO {
  const collectNum = raw.collectNum ?? raw.collectCount ?? 0
  return {
    shopId: toIdString(raw.shopId),
    collectNum,
    collectCount: collectNum,
    collected: raw.collected ?? raw.isCollected ?? false,
    isCollected: raw.isCollected ?? raw.collected ?? false,
  }
}

export function normalizeShopCollectItem(raw: ShopCollectRaw): ShopCollectVO | null {
  const shopId = toIdString(raw.shopId)
  if (!shopId) return null
  return {
    id: toIdString(raw.id),
    shopId,
    shopName: raw.shopName,
    shopAvatar: raw.shopAvatar,
    shopDesc: raw.shopDesc,
    collectNum: raw.collectNum,
    collectTime: raw.collectTime ?? raw.createTime,
    createTime: raw.createTime,
  }
}

export function normalizeShopCollectList(records: ShopCollectRaw[]): ShopCollectVO[] {
  if (!Array.isArray(records)) return []
  return records
    .map(normalizeShopCollectItem)
    .filter((item): item is ShopCollectVO => item != null)
}

/** 公开店铺列表查询（审核通过且正常营业，由后端 /shop/list 默认筛选） */
export function buildShopSearchQuery(pageNum: number, shopName?: string): ShopApplyQueryRequest {
  const keyword = shopName?.trim()
  return {
    pageNum,
    pageSize: HOME_SHOP_PAGE_SIZE,
    ...(keyword ? { shopName: keyword } : {}),
  }
}

export function normalizeShopApplyList(records: ShopApplyRaw[]): ShopApplyVO[] {
  const items = records.map(normalizeShopApplyItem)
  const duplicates = findDuplicateIds(items.map((item) => item.id))
  if (duplicates.length > 0 && import.meta.env.DEV) {
    console.error('[shop] 检测到重复 id，列表状态可能错乱:', duplicates)
  }
  return items
}

export function formatShopStatusLabel(status?: ShopStatus, desc?: string): string {
  if (desc) return desc
  if (status === ShopStatus.ENABLED || status === ShopStatus.DISABLED) {
    return SHOP_STATUS_LABELS[status]
  }
  return '--'
}

export function isShopEnabled(status?: ShopStatus): boolean {
  return status !== ShopStatus.DISABLED
}

export function getToggleShopStatus(status?: ShopStatus): ShopStatus {
  return isShopEnabled(status) ? ShopStatus.DISABLED : ShopStatus.ENABLED
}

export function canUpdateShopStatus(item: ShopApplyVO): boolean {
  return item.auditStatus === ShopAuditStatus.APPROVED && !!item.id
}

export const SHOP_NAME_MAX_LENGTH = 30
export const SHOP_DESC_MAX_LENGTH = 200
export const SHOP_AVATAR_MAX_SIZE = 5 * 1024 * 1024

export function buildCreateShopRequest(form: {
  shopName: string
  shopAvatar?: string
  shopDesc?: string
}): CreateShopRequest {
  const payload: CreateShopRequest = {
    shopName: form.shopName.trim(),
  }

  const avatar = form.shopAvatar?.trim()
  if (avatar) {
    payload.shopAvatar = avatar
  }

  const desc = form.shopDesc?.trim()
  if (desc) {
    payload.shopDesc = desc
  }

  return payload
}

export function validateCreateShopForm(form: {
  shopName: string
  shopDesc?: string
}): string | null {
  const shopName = form.shopName.trim()
  if (!shopName) return '店铺名称不能为空'
  if (shopName.length > SHOP_NAME_MAX_LENGTH) {
    return `店铺名称不能超过 ${SHOP_NAME_MAX_LENGTH} 个字符`
  }

  const desc = form.shopDesc?.trim() ?? ''
  if (desc.length > SHOP_DESC_MAX_LENGTH) {
    return `店铺简介不能超过 ${SHOP_DESC_MAX_LENGTH} 字`
  }

  return null
}
