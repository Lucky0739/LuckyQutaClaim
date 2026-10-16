import type { PageResult } from '@/types/common'

/** 站内消息 VO，对应后端 NotifyVO */
export interface Notify {
  /** 消息 ID（后端 Long，前端统一用 string 避免精度丢失） */
  id: string
  sendUserId?: number
  receiveUserId?: number
  title: string
  content: string
  /** 1=开店申请 2=商品上架申请 3=商品上架审核申请通过 4=支付成功 5=发货 6=收货 */
  notifyType: number
  notifyTypeName?: string
  /** 关联业务 ID */
  businessId?: number | string
  subType?: number
  /** 0=未读 1=已读 */
  isRead: number
  readTime?: string
  createTime: string
  updateTime?: string
  isDelete?: number
}

/** 用户消息分页结果，对应后端 NotifyListVO */
export interface NotifyListVO {
  records: Notify[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
  /** 当前筛选条件下的未读数 */
  unreadCount: number
}

/** 消息类型 Tab 汇总，对应后端 NotifyTypeSummaryVO */
export interface NotifyTypeSummaryVO {
  notifyType: number
  notifyTypeName: string
  unreadCount: number
  totalCount: number
}

/** 全部标记已读结果，对应后端 NotifyMarkAllReadVO */
export interface NotifyMarkAllReadVO {
  markedCount: number
}

/** 按类型统计未读，对应后端 NotifyUnreadStatVO */
export interface NotifyUnreadStatVO {
  receiveUserId?: number
  notifyType: number
  notifyTypeName?: string
  unreadCount: number
}

/** 消息分页查询请求（对应后端 NotifyQueryRequest） */
export interface NotifyQueryRequest {
  /** 消息类型（用户列表按 Tab 切换时传入） */
  notifyType?: number
  /** 0=未读 1=已读，不传查全部 */
  isRead?: number
  pageNum?: number
  pageSize?: number
}

export type NotifyListMode = 'admin' | 'user'

export type NotifyReadFilter = 'all' | 'unread' | 'read'

/** API 解析前的消息项 */
export type NotifyRaw = Omit<Notify, 'id' | 'businessId'> & {
  id: string | number
  businessId?: string | number
}

/** 统一转为消息 ID 字符串 */
export function toNotifyId(id: string | number): string {
  return String(id)
}

/** 规范化单条消息 */
export function normalizeNotifyItem(item: NotifyRaw): Notify {
  return {
    ...item,
    id: toNotifyId(item.id),
    businessId: item.businessId != null ? toNotifyId(item.businessId) : undefined,
  }
}

/** 规范化消息列表 */
export function normalizeNotifyList(records: NotifyRaw[]): Notify[] {
  return records.map(normalizeNotifyItem)
}

/** 规范化按类型未读统计 */
export function normalizeNotifyUnreadStatList(
  list: Array<{
    receiveUserId?: number
    notifyType?: number
    notifyTypeName?: string
    unreadCount?: number | string
  }> | null | undefined,
): NotifyUnreadStatVO[] {
  if (!list?.length) return []
  return list
    .filter((item) => item.notifyType != null)
    .map((item) => ({
      receiveUserId: item.receiveUserId,
      notifyType: item.notifyType as number,
      notifyTypeName: item.notifyTypeName,
      unreadCount: Number(item.unreadCount) || 0,
    }))
}

/** 规范化用户分页结果 */
export function normalizeNotifyListVO(data: {
  records?: NotifyRaw[]
  total?: number
  pageNum?: number
  pageSize?: number
  totalPages?: number
  unreadCount?: number
}): NotifyListVO {
  return {
    records: normalizeNotifyList(data.records ?? []),
    total: data.total ?? 0,
    pageNum: data.pageNum ?? 1,
    pageSize: data.pageSize ?? 10,
    totalPages: data.totalPages ?? 0,
    unreadCount: data.unreadCount ?? 0,
  }
}

/** 管理员列表仍使用 PageResult */
export type NotifyPageResult = PageResult<Notify>
