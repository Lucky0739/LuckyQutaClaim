/** 秒杀活动状态，与后端 SeckillActivityStatusEnum 一致 */
export const SeckillActivityStatus = {
  NOT_STARTED: 0,
  IN_PROGRESS: 1,
  ENDED: 2,
} as const
export type SeckillActivityStatus = (typeof SeckillActivityStatus)[keyof typeof SeckillActivityStatus]

export interface SeckillActivityVO {
  id: string
  activityName: string
  beginTime?: string
  endTime?: string
  status: SeckillActivityStatus
  description?: string
}

export type SeckillActivityRaw = Omit<SeckillActivityVO, 'id'> & {
  id?: string | number
}

export function normalizeSeckillActivity(raw: SeckillActivityRaw): SeckillActivityVO | null {
  const id = toIdString(raw.id)
  if (!id) return null
  return {
    id,
    activityName: raw.activityName,
    beginTime: raw.beginTime,
    endTime: raw.endTime,
    status: raw.status,
    description: raw.description,
  }
}

export function normalizeSeckillActivityList(raw: SeckillActivityRaw[]): SeckillActivityVO[] {
  if (!Array.isArray(raw)) return []
  return raw
    .map(normalizeSeckillActivity)
    .filter((item): item is SeckillActivityVO => item != null)
}

export interface SeckillGoodsVO {
  id: string
  activityId: string
  goodsId: string
  skuId: string
  seckillTitle?: string
  seckillImg?: string
  seckillPrice?: number
  seckillStock?: number
  seckillLimit?: number
  sort?: number
}

export interface SeckillRequest {
  seckillGoodsId: string
}

export interface SeckillResultVO {
  orderNo?: string
  /** -2=创建失败，-1=处理中，0=待支付；null 表示结果已过期 */
  status?: number | null
  message?: string
}

/** 秒杀商品审核状态，与后端 SeckillGoodsAuditStatusEnum 一致 */
export const SeckillGoodsAuditStatus = {
  PENDING: 1,
  APPROVED: 2,
  REJECTED: 3,
} as const
export type SeckillGoodsAuditStatus =
  (typeof SeckillGoodsAuditStatus)[keyof typeof SeckillGoodsAuditStatus]

export interface SeckillGoodsApplyRequest {
  activityId: string
  goodsId: string
  skuId: string
  seckillTitle?: string
  seckillImg?: string
  seckillPrice: number
  seckillTotal: number
  seckillLimit?: number
  sort?: number
}

export interface SeckillGoodsApplyQuery {
  activityId?: string
  auditStatus?: SeckillGoodsAuditStatus
  pageNum: number
  pageSize: number
}

/** 审核秒杀商品申请，对应后端 SeckillGoodsAuditRequest */
export interface SeckillGoodsAuditRequest {
  seckillGoodsId: string
  auditStatus:
    | typeof SeckillGoodsAuditStatus.APPROVED
    | typeof SeckillGoodsAuditStatus.REJECTED
  auditRemark?: string
}

export function canAuditSeckillGoods(status?: number): boolean {
  return status === SeckillGoodsAuditStatus.PENDING
}

export interface SeckillGoodsApplyVO {
  id: string
  activityId: string
  activityName?: string
  goodsId: string
  skuId: string
  skuSpecName?: string
  seckillTitle?: string
  seckillImg?: string
  seckillPrice?: number
  seckillTotal?: number
  seckillStock?: number
  seckillLimit?: number
  auditStatus?: SeckillGoodsAuditStatus
  auditRemark?: string
  userId?: string
  createTime?: string
  /** 当前是否可参与秒杀购买（活动已开始、进行中且库存已预热） */
  seckillPurchasable?: boolean
}

export type SeckillGoodsApplyRaw = Omit<
  SeckillGoodsApplyVO,
  'id' | 'activityId' | 'goodsId' | 'skuId' | 'userId' | 'seckillPrice'
> & {
  id?: string | number
  activityId?: string | number
  goodsId?: string | number
  skuId?: string | number
  userId?: string | number
  seckillPrice?: number | string
  seckillPurchasable?: boolean
}

const SECKILL_GOODS_AUDIT_LABELS: Record<SeckillGoodsAuditStatus, string> = {
  [SeckillGoodsAuditStatus.PENDING]: '待审核',
  [SeckillGoodsAuditStatus.APPROVED]: '已通过',
  [SeckillGoodsAuditStatus.REJECTED]: '已拒绝',
}

export function formatSeckillGoodsAuditStatus(status?: number): string {
  if (status == null) return '--'
  return SECKILL_GOODS_AUDIT_LABELS[status as SeckillGoodsAuditStatus] ?? `状态${status}`
}

export function normalizeSeckillGoodsApply(raw: SeckillGoodsApplyRaw): SeckillGoodsApplyVO | null {
  const id = toIdString(raw.id)
  const activityId = toIdString(raw.activityId)
  const goodsId = toIdString(raw.goodsId)
  const skuId = toIdString(raw.skuId)
  if (!id || !activityId || !goodsId || !skuId) return null

  return {
    id,
    activityId,
    activityName: raw.activityName,
    goodsId,
    skuId,
    skuSpecName: raw.skuSpecName,
    seckillTitle: raw.seckillTitle,
    seckillImg: raw.seckillImg,
    seckillPrice: toNumber(raw.seckillPrice),
    seckillTotal: raw.seckillTotal,
    seckillStock: raw.seckillStock,
    seckillLimit: raw.seckillLimit,
    auditStatus: raw.auditStatus as SeckillGoodsAuditStatus | undefined,
    auditRemark: raw.auditRemark,
    userId: toIdString(raw.userId),
    createTime: raw.createTime,
    seckillPurchasable: raw.seckillPurchasable,
  }
}

export function normalizeSeckillGoodsApplyList(records: SeckillGoodsApplyRaw[]): SeckillGoodsApplyVO[] {
  if (!Array.isArray(records)) return []
  return records
    .map(normalizeSeckillGoodsApply)
    .filter((item): item is SeckillGoodsApplyVO => item != null)
}

export type SeckillGoodsRaw = Omit<
  SeckillGoodsVO,
  'id' | 'activityId' | 'goodsId' | 'skuId' | 'seckillPrice'
> & {
  id?: string | number
  activityId?: string | number
  goodsId?: string | number
  skuId?: string | number
  seckillPrice?: number | string
}

function toIdString(value?: string | number | null): string {
  if (value === undefined || value === null || value === '') return ''
  return String(value)
}

function toNumber(value?: number | string | null): number | undefined {
  if (value === undefined || value === null || value === '') return undefined
  const num = Number(value)
  return Number.isFinite(num) ? num : undefined
}

export function normalizeSeckillGoods(raw: SeckillGoodsRaw): SeckillGoodsVO | null {
  const id = toIdString(raw.id)
  const activityId = toIdString(raw.activityId)
  const goodsId = toIdString(raw.goodsId)
  const skuId = toIdString(raw.skuId)
  if (!id || !activityId || !goodsId || !skuId) return null

  return {
    id,
    activityId,
    goodsId,
    skuId,
    seckillTitle: raw.seckillTitle,
    seckillImg: raw.seckillImg,
    seckillPrice: toNumber(raw.seckillPrice),
    seckillStock: raw.seckillStock,
    seckillLimit: raw.seckillLimit,
    sort: raw.sort,
  }
}

export function normalizeSeckillGoodsList(records: SeckillGoodsRaw[]): SeckillGoodsVO[] {
  if (!Array.isArray(records)) return []
  return records
    .map(normalizeSeckillGoods)
    .filter((item): item is SeckillGoodsVO => item != null)
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
}

/** 首页展示优先进行中的活动，其次未开始 */
export function pickDisplayActivity(
  activities: SeckillActivityVO[],
): SeckillActivityVO | null {
  if (!activities.length) return null
  const inProgress = activities.find((item) => item.status === SeckillActivityStatus.IN_PROGRESS)
  if (inProgress) return inProgress
  const notStarted = activities.find((item) => item.status === SeckillActivityStatus.NOT_STARTED)
  if (notStarted) return notStarted
  return activities[0] ?? null
}

/** 秒杀页默认展示的活动优先级：进行中 → 未开始 → 已结束 */
export function orderActivitiesForDisplay(
  activities: SeckillActivityVO[],
): SeckillActivityVO[] {
  const priority = [
    SeckillActivityStatus.IN_PROGRESS,
    SeckillActivityStatus.NOT_STARTED,
    SeckillActivityStatus.ENDED,
  ] as const
  return priority.flatMap((status) => activities.filter((item) => item.status === status))
}

export function getActivityCountdownTarget(activity: SeckillActivityVO | null): {
  label: string
  targetTime?: string
} {
  if (!activity) return { label: '距结束' }
  if (activity.status === SeckillActivityStatus.NOT_STARTED) {
    return { label: '距开始', targetTime: activity.beginTime }
  }
  if (activity.status === SeckillActivityStatus.ENDED) {
    return { label: '已结束' }
  }
  return { label: '距结束', targetTime: activity.endTime }
}

export function calcRemainingSeconds(targetTime?: string): number {
  if (!targetTime) return 0
  const target = new Date(targetTime).getTime()
  if (Number.isNaN(target)) return 0
  return Math.max(0, Math.floor((target - Date.now()) / 1000))
}

export function formatSeckillPrice(price?: number): string {
  if (price == null || !Number.isFinite(price)) return '--'
  return price.toFixed(2)
}

const STATUS_LABELS: Record<SeckillActivityStatus, string> = {
  [SeckillActivityStatus.NOT_STARTED]: '未开始',
  [SeckillActivityStatus.IN_PROGRESS]: '进行中',
  [SeckillActivityStatus.ENDED]: '已结束',
}

export function formatActivityStatus(status: SeckillActivityStatus): string {
  return STATUS_LABELS[status] ?? '未知'
}

export function isActivityEnded(status: SeckillActivityStatus): boolean {
  return status === SeckillActivityStatus.ENDED
}

export function isActivityInProgress(status: SeckillActivityStatus): boolean {
  return status === SeckillActivityStatus.IN_PROGRESS
}

/** 秒杀活动已开始（进行中）时才允许参与秒杀购买 */
export function canPurchaseSeckillGoods(activity: SeckillActivityVO | null | undefined): boolean {
  if (!activity) return false
  return activity.status === SeckillActivityStatus.IN_PROGRESS
}

export function getSeckillPurchaseButtonText(
  activity: SeckillActivityVO | null | undefined,
  options?: { outOfStock?: boolean; purchasable?: boolean },
): string {
  if (options?.outOfStock) return '已抢光'
  if (!activity) return '加载中...'
  if (activity.status === SeckillActivityStatus.ENDED) return '活动已结束'
  if (activity.status === SeckillActivityStatus.NOT_STARTED) return '即将开抢'
  if (options?.purchasable === false) return '库存预热中'
  return '立即秒杀'
}

/** 未开始的活动可手动开始 */
export function canStartActivity(activity: SeckillActivityVO): boolean {
  return activity.status === SeckillActivityStatus.NOT_STARTED
}

/** 未开始或进行中的活动可手动结束 */
export function canEndActivity(activity: SeckillActivityVO): boolean {
  return activity.status !== SeckillActivityStatus.ENDED
}

// ======================== 死信队列失败订单 ========================

/** DLQ失败订单处理状态 */
export const FailedOrderHandleStatus = {
  /** 未处理 */
  PENDING: 0,
  /** 已重试 */
  RETRIED: 1,
  /** 已放弃 */
  DISCARDED: 2,
} as const
export type FailedOrderHandleStatus =
  (typeof FailedOrderHandleStatus)[keyof typeof FailedOrderHandleStatus]

const FAILED_ORDER_HANDLE_LABELS: Record<FailedOrderHandleStatus, string> = {
  [FailedOrderHandleStatus.PENDING]: '未处理',
  [FailedOrderHandleStatus.RETRIED]: '已重试',
  [FailedOrderHandleStatus.DISCARDED]: '已放弃',
}

export function formatFailedOrderHandleStatus(status?: number | null): string {
  if (status == null) return '--'
  return FAILED_ORDER_HANDLE_LABELS[status as FailedOrderHandleStatus] ?? `状态${status}`
}

/** 是否可对失败订单执行操作（仅未处理状态可操作） */
export function canHandleFailedOrder(status?: number | null): boolean {
  return status === FailedOrderHandleStatus.PENDING
}

export interface FailedOrderVO {
  id: string
  userId?: string
  activityId?: string
  goodsId?: string
  skuId?: string
  seckillGoodsId?: string
  orderNo?: string
  seckillPrice?: number
  goodsName?: string
  goodsImg?: string
  buyNum?: number
  lastError?: string
  retryCount?: number
  handleStatus?: number
  handleStatusText?: string
  handleRemark?: string
  handledBy?: string
  handledTime?: string
  createTime?: string
  updateTime?: string
}

export type FailedOrderRaw = Omit<
  FailedOrderVO,
  'id' | 'userId' | 'activityId' | 'goodsId' | 'skuId' | 'seckillGoodsId'
> & {
  id?: string | number
  userId?: string | number
  activityId?: string | number
  goodsId?: string | number
  skuId?: string | number
  seckillGoodsId?: string | number
}

export function normalizeFailedOrder(raw: FailedOrderRaw): FailedOrderVO | null {
  const id = toIdString(raw.id)
  if (!id) return null

  return {
    id,
    userId: toIdString(raw.userId),
    activityId: toIdString(raw.activityId),
    goodsId: toIdString(raw.goodsId),
    skuId: toIdString(raw.skuId),
    seckillGoodsId: toIdString(raw.seckillGoodsId),
    orderNo: raw.orderNo,
    seckillPrice: toNumber(raw.seckillPrice),
    goodsName: raw.goodsName,
    goodsImg: raw.goodsImg,
    buyNum: raw.buyNum,
    lastError: raw.lastError,
    retryCount: raw.retryCount,
    handleStatus: raw.handleStatus,
    handleStatusText: raw.handleStatusText,
    handleRemark: raw.handleRemark,
    handledBy: raw.handledBy,
    handledTime: raw.handledTime,
    createTime: raw.createTime,
    updateTime: raw.updateTime,
  }
}

export function normalizeFailedOrderList(records: FailedOrderRaw[]): FailedOrderVO[] {
  if (!Array.isArray(records)) return []
  return records
    .map(normalizeFailedOrder)
    .filter((item): item is FailedOrderVO => item != null)
}
