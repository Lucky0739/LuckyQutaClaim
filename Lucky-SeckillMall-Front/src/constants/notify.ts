import type { RouteRecordRaw } from 'vue-router'
import { isAdmin } from '@/utils/auth'
import type { NotifyListMode } from '@/types/notify'

/**
 * 消息类型（消息中心 Tab 与接口 notifyType 一致）
 * 1=开店申请 2=商品上架申请 3=商品上架审核申请通过
 * 4=支付成功通知 5=发货通知 6=收货通知
 */
export const NOTIFY_TYPE = {
  MERCHANT_APPLY: 1,
  GOODS_LISTING_APPLY: 2,
  GOODS_LISTING_APPROVED: 3,
  PAY_SUCCESS: 4,
  SHIPMENT: 5,
  RECEIPT: 6,
} as const

export type NotifyTypeValue = (typeof NOTIFY_TYPE)[keyof typeof NOTIFY_TYPE]

/** 消息类型展示名称 */
export const NOTIFY_TYPE_LABEL: Record<NotifyTypeValue, string> = {
  [NOTIFY_TYPE.MERCHANT_APPLY]: '开店申请',
  [NOTIFY_TYPE.GOODS_LISTING_APPLY]: '商品上架申请',
  [NOTIFY_TYPE.GOODS_LISTING_APPROVED]: '商品上架审核申请通过',
  [NOTIFY_TYPE.PAY_SUCCESS]: '支付成功通知',
  [NOTIFY_TYPE.SHIPMENT]: '发货通知',
  [NOTIFY_TYPE.RECEIPT]: '收货通知',
}

export interface NotifyCategory {
  type: NotifyTypeValue
  label: string
  routeName: string
  path: string
}

/** 全部消息类型（用户按 notifyType 数字切换 Tab） */
export const NOTIFY_CATEGORIES: NotifyCategory[] = [
  {
    type: NOTIFY_TYPE.MERCHANT_APPLY,
    label: NOTIFY_TYPE_LABEL[NOTIFY_TYPE.MERCHANT_APPLY],
    routeName: 'NotifyShopApply',
    path: '/messages/shop-apply',
  },
  {
    type: NOTIFY_TYPE.GOODS_LISTING_APPLY,
    label: NOTIFY_TYPE_LABEL[NOTIFY_TYPE.GOODS_LISTING_APPLY],
    routeName: 'NotifyGoodsListingApply',
    path: '/messages/goods-apply',
  },
  {
    type: NOTIFY_TYPE.GOODS_LISTING_APPROVED,
    label: NOTIFY_TYPE_LABEL[NOTIFY_TYPE.GOODS_LISTING_APPROVED],
    routeName: 'NotifyGoodsListingApproved',
    path: '/messages/goods-approved',
  },
  {
    type: NOTIFY_TYPE.PAY_SUCCESS,
    label: NOTIFY_TYPE_LABEL[NOTIFY_TYPE.PAY_SUCCESS],
    routeName: 'NotifyPaySuccess',
    path: '/messages/pay-success',
  },
  {
    type: NOTIFY_TYPE.SHIPMENT,
    label: NOTIFY_TYPE_LABEL[NOTIFY_TYPE.SHIPMENT],
    routeName: 'NotifyShipment',
    path: '/messages/shipment',
  },
  {
    type: NOTIFY_TYPE.RECEIPT,
    label: NOTIFY_TYPE_LABEL[NOTIFY_TYPE.RECEIPT],
    routeName: 'NotifyReceipt',
    path: '/messages/receipt',
  },
]

/** 管理员消息中心仅展示需审核的类型 */
/** 管理员消息中心：需审核的类型（1 开店申请、2 商品上架申请） */
const ADMIN_NOTIFY_TYPES = new Set<NotifyTypeValue>([
  NOTIFY_TYPE.MERCHANT_APPLY,
  NOTIFY_TYPE.GOODS_LISTING_APPLY,
])

export function isAdminNotifyType(type: number): boolean {
  return ADMIN_NOTIFY_TYPES.has(type as NotifyTypeValue)
}

/** 当前角色下可见的消息分类 Tab */
export function getVisibleNotifyCategories(isAdminUser = isAdmin()): NotifyCategory[] {
  if (isAdminUser) {
    return NOTIFY_CATEGORIES.filter((item) => ADMIN_NOTIFY_TYPES.has(item.type))
  }
  return NOTIFY_CATEGORIES
}

/** 列表查询模式：管理员审核类走 admin/list，其余走 user/list */
export function getNotifyListMode(notifyType: number, isAdminUser = isAdmin()): NotifyListMode {
  if (isAdminUser && isAdminNotifyType(notifyType)) return 'admin'
  return 'user'
}

export function getNotifyCategoryByRouteName(routeName: string): NotifyCategory | undefined {
  return NOTIFY_CATEGORIES.find((item) => item.routeName === routeName)
}

export function getNotifyCategoryByType(type: number): NotifyCategory | undefined {
  return NOTIFY_CATEGORIES.find((item) => item.type === type)
}

/** 按 notifyType 数字解析展示名称 */
export function getNotifyTypeLabel(type: number): string {
  return NOTIFY_TYPE_LABEL[type as NotifyTypeValue] ?? `消息类型${type}`
}

export function getDefaultNotifyRouteName(isAdminUser = isAdmin()): string {
  return getVisibleNotifyCategories(isAdminUser)[0]?.routeName ?? 'NotifyShopApply'
}

export function getDefaultNotifyPath(isAdminUser = isAdmin()): string {
  return getVisibleNotifyCategories(isAdminUser)[0]?.path ?? '/messages/shop-apply'
}

/** 按消息类型解析消息中心路径 */
export function getNotifyMessagePath(notifyType: number, isAdminUser = isAdmin()): string {
  const category = NOTIFY_CATEGORIES.find((item) => item.type === notifyType)
  if (!category) return getDefaultNotifyPath(isAdminUser)
  if (isAdminUser && !ADMIN_NOTIFY_TYPES.has(category.type)) {
    return getDefaultNotifyPath(isAdminUser)
  }
  return category.path
}

/** 开店申请类消息标记已读走专用接口（管理员场景） */
export function isMerchantApplyNotifyType(notifyType: number): boolean {
  return notifyType === NOTIFY_TYPE.MERCHANT_APPLY
}

/** 管理员审核类消息（按 businessId 标记已读） */
export function isAdminApplyNotifyType(notifyType: number): boolean {
  return notifyType === NOTIFY_TYPE.MERCHANT_APPLY
    || notifyType === NOTIFY_TYPE.GOODS_LISTING_APPLY
}

export function buildNotifyRoutes(): RouteRecordRaw[] {
  return NOTIFY_CATEGORIES.map((category) => ({
    path: category.path.replace(/^\/messages\//, ''),
    name: category.routeName,
    component: () => import('@/views/messages/NotifyListPanel.vue'),
    meta: {
      title: category.label,
      notifyType: category.type,
      requiresAuth: true,
    },
  }))
}
