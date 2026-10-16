import request, { get, post, postVoid } from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/common'
import type {
  SeckillActivityRaw,
  SeckillActivityVO,
  SeckillGoodsApplyQuery,
  SeckillGoodsApplyRaw,
  SeckillGoodsApplyRequest,
  SeckillGoodsApplyVO,
  SeckillGoodsAuditRequest,
  SeckillGoodsRaw,
  SeckillGoodsVO,
  SeckillRequest,
  SeckillResultVO,
} from '@/types/activity'
import {
  normalizeSeckillActivity,
  normalizeSeckillActivityList,
  normalizeSeckillGoodsApplyList,
  normalizeSeckillGoodsList,
  orderActivitiesForDisplay,
  pickDisplayActivity,
  SeckillActivityStatus,
} from '@/types/activity'
import { parseJsonPreservingLongIds } from '@/utils/json'

const BASE = '/activity'

const SECKILL_LONG_ID_FIELDS = ['id', 'activityId', 'goodsId', 'skuId', 'userId', 'seckillGoodsId']

function parseActivityResponse(raw: string): ApiResponse<SeckillActivityRaw | SeckillActivityRaw[]> {
  if (!raw) return { success: true, data: [] }
  try {
    return parseJsonPreservingLongIds<ApiResponse<SeckillActivityRaw | SeckillActivityRaw[]>>(raw, ['id'])
  } catch {
    return JSON.parse(raw) as ApiResponse<SeckillActivityRaw | SeckillActivityRaw[]>
  }
}

/** 构建秒杀申请请求体，字段与后端 SeckillGoodsApplyRequest 一致 */
function buildSeckillGoodsApplyBody(data: SeckillGoodsApplyRequest) {
  return {
    activityId: data.activityId,
    goodsId: data.goodsId,
    skuId: data.skuId,
    seckillTitle: data.seckillTitle,
    seckillImg: data.seckillImg,
    seckillPrice: data.seckillPrice,
    seckillTotal: data.seckillTotal,
    seckillLimit: data.seckillLimit ?? 1,
    sort: data.sort ?? 0,
  }
}

/** 秒杀活动列表 GET /activity/list */
export async function getActivityList(): Promise<SeckillActivityVO[]> {
  const response = await request.get(`${BASE}/list`, {
    transformResponse: [(raw: string) => parseActivityResponse(raw)],
  })

  const res = response.data as ApiResponse<SeckillActivityRaw[]>
  if (res.success === false) {
    throw new Error(res.message || '加载秒杀活动列表失败')
  }
  return normalizeSeckillActivityList(res.data ?? [])
}

/** 秒杀活动详情 GET /activity/{activityId} */
export async function getActivityDetail(activityId: number | string): Promise<SeckillActivityVO> {
  const response = await request.get(`${BASE}/${activityId}`, {
    transformResponse: [(raw: string) => parseActivityResponse(raw)],
  })

  const res = response.data as ApiResponse<SeckillActivityRaw>
  if (res.success === false) {
    throw new Error(res.message || '加载秒杀活动详情失败')
  }
  if (!res.data) {
    throw new Error('秒杀活动不存在')
  }
  const activity = normalizeSeckillActivity(res.data)
  if (!activity) {
    throw new Error('秒杀活动不存在')
  }
  return activity
}

/** 秒杀活动商品列表 GET /activity/{activityId}/goods */
export async function getActivityGoods(activityId: number | string): Promise<SeckillGoodsVO[]> {
  const response = await request.get(`${BASE}/${activityId}/goods`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<SeckillGoodsRaw[]>>(
          raw,
          SECKILL_LONG_ID_FIELDS,
        )
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<SeckillGoodsRaw[]>
  if (res.success === false) {
    throw new Error(res.message || '加载秒杀商品失败')
  }
  return normalizeSeckillGoodsList(res.data ?? [])
}

/**
 * 解析秒杀页默认展示的活动与商品。
 * 优先展示「进行中/未开始且已有审核通过商品」的活动，避免选中空活动导致审核后仍看不到商品。
 */
export async function resolveSeckillDisplayPage(): Promise<{
  activity: SeckillActivityVO
  goods: SeckillGoodsVO[]
} | null> {
  const activities = await getActivityList()
  if (!activities.length) return null

  const candidates = orderActivitiesForDisplay(activities).filter(
    (item) =>
      item.status === SeckillActivityStatus.IN_PROGRESS
      || item.status === SeckillActivityStatus.NOT_STARTED,
  )

  const results = await Promise.all(
    candidates.map(async (item) => ({
      activity: item,
      goods: await getActivityGoods(item.id),
    })),
  )

  const matched = results.find((item) => item.goods.length > 0)
  if (matched) return matched

  const fallback = pickDisplayActivity(activities)
  if (!fallback) return null

  const cached = results.find((item) => item.activity.id === fallback.id)
  if (cached) return cached

  return {
    activity: fallback,
    goods: await getActivityGoods(fallback.id),
  }
}

/** 参与秒杀 POST /activity/seckill */
export function doSeckill(data: SeckillRequest) {
  return post<SeckillResultVO>(`${BASE}/seckill`, {
    seckillGoodsId: data.seckillGoodsId,
  })
}

/** 查询秒杀订单异步创建结果 GET /activity/seckill/result */
export function getSeckillResult(orderNo: string) {
  return get<SeckillResultVO>(`${BASE}/seckill/result`, {
    params: { orderNo },
  })
}

/** 商家申请加入秒杀商品 POST /activity/seckill-goods/apply */
export async function applySeckillGoods(data: SeckillGoodsApplyRequest) {
  const response = await request.post(`${BASE}/seckill-goods/apply`, buildSeckillGoodsApplyBody(data), {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        const parsed = parseJsonPreservingLongIds<ApiResponse<string | number>>(raw, ['data'])
        if (parsed.data != null) {
          parsed.data = String(parsed.data) as unknown as string
        }
        return parsed
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<string>
  if (res.success === false) {
    throw new Error(res.message || '秒杀商品申请失败')
  }
  return String(res.data ?? '')
}

/** 审核秒杀商品申请 POST /activity/seckill-goods/audit */
export async function auditSeckillGoods(data: SeckillGoodsAuditRequest) {
  const response = await request.post(`${BASE}/seckill-goods/audit`, {
    seckillGoodsId: data.seckillGoodsId,
    auditStatus: data.auditStatus,
    auditRemark: data.auditRemark,
  })

  const res = response.data as ApiResponse<unknown>
  if (res.success === false) {
    throw new Error(res.message || '秒杀商品审核失败')
  }
}

/** 分页查询秒杀商品申请 GET /activity/seckill-goods/apply/page */
export async function listSeckillGoodsApply(query: SeckillGoodsApplyQuery) {
  const response = await request.get(`${BASE}/seckill-goods/apply/page`, {
    params: {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      activityId: query.activityId || undefined,
      auditStatus: query.auditStatus,
    },
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<SeckillGoodsApplyRaw>>>(
          raw,
          SECKILL_LONG_ID_FIELDS,
        )
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<PageResult<SeckillGoodsApplyRaw>>
  if (res.success === false) {
    throw new Error(res.message || '加载秒杀申请列表失败')
  }

  const page = res.data ?? {
    records: [],
    total: 0,
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    totalPages: 0,
  }

  return {
    ...page,
    total: Number(page.total ?? 0),
    pageNum: Number(page.pageNum ?? query.pageNum),
    pageSize: Number(page.pageSize ?? query.pageSize),
    totalPages: Number(page.totalPages ?? 0),
    records: normalizeSeckillGoodsApplyList(page.records ?? []),
  } satisfies PageResult<SeckillGoodsApplyVO>
}

/** 秒杀商品详情 GET /activity/seckill-goods/{seckillGoodsId} */
export async function getSeckillGoodsDetail(seckillGoodsId: string) {
  const response = await request.get(`${BASE}/seckill-goods/${seckillGoodsId}`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<SeckillGoodsApplyRaw>>(
          raw,
          SECKILL_LONG_ID_FIELDS,
        )
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<SeckillGoodsApplyRaw>
  if (res.success === false) {
    throw new Error(res.message || '加载秒杀商品详情失败')
  }
  const list = normalizeSeckillGoodsApplyList(res.data ? [res.data] : [])
  if (!list[0]) {
    throw new Error('秒杀商品不存在')
  }
  return list[0]
}

/** 手动开始秒杀活动 POST /activity/{activityId}/start */
export async function startActivity(activityId: number | string): Promise<SeckillActivityVO> {
  const response = await request.post(`${BASE}/${activityId}/start`, undefined, {
    transformResponse: [(raw: string) => parseActivityResponse(raw)],
  })
  const res = response.data as ApiResponse<SeckillActivityRaw>
  if (res.success === false) {
    throw new Error(res.message || '开始秒杀活动失败')
  }
  if (!res.data) {
    throw new Error('开始秒杀活动失败')
  }
  const activity = normalizeSeckillActivity(res.data)
  if (!activity) {
    throw new Error('开始秒杀活动失败')
  }
  return activity
}

/** 手动结束秒杀活动 POST /activity/{activityId}/end */
export function endActivity(activityId: number | string) {
  return post<boolean>(`${BASE}/${activityId}/end`)
}

// ======================== 死信队列失败订单 ========================

import type { FailedOrderRaw, FailedOrderVO } from '@/types/activity'
import { normalizeFailedOrderList } from '@/types/activity'

const FAILED_ORDER_LONG_ID_FIELDS = ['id', 'userId', 'activityId', 'goodsId', 'skuId', 'seckillGoodsId']

/** 查询DLQ失败订单列表 GET /activity/admin/failed-orders */
export async function listFailedOrders(params: {
  handleStatus?: number
  pageNum?: number
  pageSize?: number
}): Promise<PageResult<FailedOrderVO>> {
  const response = await request.get(`${BASE}/admin/failed-orders`, {
    params: {
      handleStatus: params.handleStatus,
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 10,
    },
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<FailedOrderRaw>>>(
          raw,
          FAILED_ORDER_LONG_ID_FIELDS,
        )
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<PageResult<FailedOrderRaw>>
  if (res.success === false) {
    throw new Error(res.message || '加载失败订单列表失败')
  }

  const page = res.data ?? {
    records: [],
    total: 0,
    pageNum: params.pageNum ?? 1,
    pageSize: params.pageSize ?? 10,
    totalPages: 0,
  }

  return {
    ...page,
    total: Number(page.total ?? 0),
    pageNum: Number(page.pageNum ?? params.pageNum ?? 1),
    pageSize: Number(page.pageSize ?? params.pageSize ?? 10),
    totalPages: Number(page.totalPages ?? 0),
    records: normalizeFailedOrderList(page.records ?? []),
  } satisfies PageResult<FailedOrderVO>
}

/** 手动重试DLQ失败订单 POST /activity/admin/failed-orders/{id}/retry */
export function manualRetryFailedOrder(id: string) {
  return postVoid(`${BASE}/admin/failed-orders/${id}/retry`)
}

/** 手动放弃DLQ失败订单 POST /activity/admin/failed-orders/{id}/discard */
export function manualDiscardFailedOrder(id: string, remark?: string) {
  return postVoid(`${BASE}/admin/failed-orders/${id}/discard`, undefined, {
    params: { remark: remark || '' },
  })
}

export type {
  FailedOrderVO,
  SeckillActivityVO,
  SeckillGoodsApplyRequest,
  SeckillGoodsApplyVO,
  SeckillGoodsAuditRequest,
  SeckillGoodsVO,
  SeckillRequest,
  SeckillResultVO,
}
