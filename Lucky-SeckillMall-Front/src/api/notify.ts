import request, { get, put, putVoid } from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/common'
import {
  normalizeNotifyItem,
  normalizeNotifyList,
  normalizeNotifyListVO,
  normalizeNotifyUnreadStatList,
  type NotifyListVO,
  type NotifyMarkAllReadVO,
  type NotifyQueryRequest,
  type NotifyRaw,
  type NotifyTypeSummaryVO,
  type NotifyUnreadStatVO,
} from '@/types/notify'

const BASE = '/notify'

/** 解析含 Long 类型 id 的 JSON */
function parseNotifyJson<T>(raw: string): ApiResponse<T> {
  const fixed = raw
    .replace(/"id"\s*:\s*(\d+)/g, '"id":"$1"')
    .replace(/"businessId"\s*:\s*(\d+)/g, '"businessId":"$1"')
    .replace(/"notifyId"\s*:\s*(\d+)/g, '"notifyId":"$1"')
  return JSON.parse(fixed) as ApiResponse<T>
}

function createNotifyTransform<T>() {
  return [(raw: string) => {
    if (typeof raw !== 'string' || !raw) return raw
    try {
      return parseNotifyJson<T>(raw)
    } catch {
      return JSON.parse(raw)
    }
  }]
}

async function fetchAdminNotifyPage(data: NotifyQueryRequest) {
  const response = await request.post(`${BASE}/admin/list`, data, {
    transformResponse: createNotifyTransform<PageResult<NotifyRaw>>(),
  })

  const res = response.data as ApiResponse<PageResult<NotifyRaw>>
  if (res.success === false) {
    throw new Error(res.message || '加载消息列表失败')
  }

  const page = res.data as PageResult<NotifyRaw>
  return {
    ...page,
    records: normalizeNotifyList(page.records ?? []),
    unreadCount: 0,
  } satisfies NotifyListVO
}

async function fetchUserNotifyPage(data: NotifyQueryRequest) {
  const response = await request.post(`${BASE}/user/list`, data, {
    transformResponse: createNotifyTransform<NotifyListVO & { records?: NotifyRaw[] }>(),
  })

  const res = response.data as ApiResponse<NotifyListVO & { records?: NotifyRaw[] }>
  if (res.success === false) {
    throw new Error(res.message || '加载消息列表失败')
  }

  return normalizeNotifyListVO(res.data ?? {})
}

/** 管理员分页查询消息列表 */
export function getAdminNotifyList(data: NotifyQueryRequest) {
  if (data.notifyType == null) {
    return Promise.reject(new Error('消息类型不能为空'))
  }
  return fetchAdminNotifyPage(data)
}

/** 用户分页查询自己收到的消息（POST，登录用户由后端解析） */
export function getUserNotifyList(data: NotifyQueryRequest) {
  return fetchUserNotifyPage(data)
}

/** 用户消息详情 GET /notify/user/detail?notifyId= */
export async function getUserNotifyDetail(notifyId: string) {
  const response = await request.get(`${BASE}/user/detail`, {
    params: { notifyId },
    transformResponse: createNotifyTransform<NotifyRaw>(),
  })
  const res = response.data as ApiResponse<NotifyRaw>
  if (res.success === false) {
    throw new Error(res.message || '加载消息详情失败')
  }
  if (!res.data) {
    throw new Error('消息不存在')
  }
  return normalizeNotifyItem(res.data)
}

/** 用户未读消息数 GET /notify/user/unread/count */
export function countUserUnreadNotify(notifyType?: number) {
  return get<number>(`${BASE}/user/unread/count`, {
    params: notifyType != null ? { notifyType } : undefined,
  })
}

/** 用户各消息类型 Tab 汇总 GET /notify/user/summary */
export function getUserNotifyTypeSummary() {
  return get<NotifyTypeSummaryVO[]>(`${BASE}/user/summary`)
}

/** 按类型统计当前用户未读 GET /notify/user/unread/stats */
export async function getUserUnreadStatsByType(userId?: number) {
  const list = await get<NotifyUnreadStatVO[]>(`${BASE}/user/unread/stats`, {
    params: userId != null ? { userId } : undefined,
  })
  return normalizeNotifyUnreadStatList(list ?? [])
}

/** 将消息标记为已读 PUT /notify/read */
export function markNotifyRead(notifyId: string) {
  return putVoid(`${BASE}/read`, null, {
    params: { notifyId },
  })
}

/** 将审核类消息标记为已读（管理员场景，按 businessId） */
export function markApplyNotifyRead(businessId: string, notifyType: number) {
  return putVoid(`${BASE}/apply/read`, null, {
    params: { businessId, notifyType },
  })
}

/** 当前用户全部消息标记已读 PUT /notify/user/read/all */
export function markAllUserNotifyRead(notifyType?: number) {
  return put<NotifyMarkAllReadVO>(`${BASE}/user/read/all`, null, {
    params: notifyType != null ? { notifyType } : undefined,
  })
}

/** 管理员将全部审核类消息标记已读 PUT /notify/admin/read/all */
export function markAllAdminNotifyRead(notifyType?: number) {
  return put<NotifyMarkAllReadVO>(`${BASE}/admin/read/all`, null, {
    params: notifyType != null ? { notifyType } : undefined,
  })
}
