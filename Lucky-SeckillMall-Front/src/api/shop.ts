import request, { delVoid, postVoid } from '@/utils/request'
import type { ApiResponse, PageResult, UploadPictureResult } from '@/types/common'
import type {
  CreateShopRequest,
  ShopApplyQueryInput,
  ShopApplyRaw,
  ShopApplyVO,
  ShopAuditRequest,
  ShopCollectRaw,
  ShopCollectRequest,
  ShopCollectStatusRaw,
  ShopCollectStatusVO,
  ShopCollectVO,
  ShopListRaw,
  ShopListVO,
  UpdateShopStatusParams,
} from '@/types/shop'
import {
  buildShopApplyQueryRequest,
  normalizeShopApplyList,
  normalizeShopCollectList,
  normalizeShopCollectStatus,
  normalizeShopListItem,
  normalizeShopList,
} from '@/types/shop'
import { API_BASE_URL } from '@/config'
import { getToken, TOKEN_KEY } from '@/utils/auth'
import { parseJsonPreservingLongIds } from '@/utils/json'

const BASE = '/shop'

async function parseShopApiResponse<T>(response: Response): Promise<T> {
  const text = await response.text()
  if (!text) {
    throw new Error('服务器返回空响应')
  }

  let parsed: ApiResponse<T>
  try {
    parsed = parseJsonPreservingLongIds<ApiResponse<T>>(text)
  } catch {
    throw new Error('响应解析失败')
  }

  if (parsed.success === false) {
    throw new Error(parsed.message || '请求失败')
  }

  return parsed.data as T
}

function buildAuthHeaders(extra?: HeadersInit): Headers {
  const headers = new Headers(extra)
  const token = getToken()
  if (token) {
    headers.set(TOKEN_KEY, token)
  }
  return headers
}

async function shopAdminRequest<T>(path: string, init: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: buildAuthHeaders(init.headers),
  })

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error(`接口不存在 (${path})，请确认网关与 shop 服务已启动`)
    }
    throw new Error(`请求失败 (${response.status})`)
  }

  return parseShopApiResponse<T>(response)
}

async function shopAdminVoid(path: string, init: RequestInit): Promise<void> {
  await shopAdminRequest<unknown>(path, init)
}

async function fetchShopPage(url: string, data: ShopApplyQueryInput) {
  const page = await shopAdminRequest<PageResult<ShopApplyRaw>>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(buildShopApplyQueryRequest(data)),
  })

  return {
    ...page,
    total: Number(page.total ?? 0),
    records: normalizeShopApplyList(page.records ?? []),
  } satisfies PageResult<ShopApplyVO>
}

/** 上传店铺头像 POST /shop/avatar/upload */
export function uploadShopAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return shopAdminRequest<UploadPictureResult>(`${BASE}/avatar/upload`, {
    method: 'POST',
    body: formData,
  })
}

/** 申请开店 POST /shop/create */
export function createShop(data: CreateShopRequest) {
  return postVoid(`${BASE}/create`, data)
}

/** 公开分页查询店铺列表 POST /shop/list */
export async function getPublicShopList(data: ShopApplyQueryInput) {
  const response = await request.post(`${BASE}/list`, buildShopApplyQueryRequest(data), {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<ShopListRaw>>>(raw, ['id'])
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<PageResult<ShopListRaw>>
  if (res.success === false) {
    throw new Error(res.message || '加载店铺列表失败')
  }

  const page = res.data ?? { records: [], total: 0, pageNum: 1, pageSize: 10, totalPages: 0 }
  return {
    ...page,
    total: Number(page.total ?? 0),
    pageNum: Number(page.pageNum ?? data.pageNum),
    pageSize: Number(page.pageSize ?? data.pageSize),
    totalPages: Number(page.totalPages ?? 0),
    records: normalizeShopList(page.records ?? []),
  } satisfies PageResult<ShopListVO>
}

/** 查询店铺详情 GET /shop/{shopId} */
export async function getShopDetail(shopId: string): Promise<ShopListVO> {
  const response = await request.get(`${BASE}/${shopId}`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<ShopListRaw>>(raw, ['id'])
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<ShopListRaw>
  if (res.success === false) {
    throw new Error(res.message || '加载店铺信息失败')
  }
  if (!res.data) {
    throw new Error('店铺不存在')
  }
  return normalizeShopListItem(res.data)
}

/** 商家查看自己的店铺列表 GET /shop/my/list */
export async function getMyShopList() {
  const response = await request.get(`${BASE}/my/list`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<ShopApplyRaw>>>(raw)
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<PageResult<ShopApplyRaw>>
  if (res.success === false) {
    throw new Error(res.message || '加载店铺列表失败')
  }

  const page = res.data ?? { records: [], total: 0, pageNum: 1, pageSize: 10, totalPages: 0 }
  return {
    ...page,
    total: Number(page.total ?? 0),
    records: normalizeShopApplyList(page.records ?? []),
  } satisfies PageResult<ShopApplyVO>
}

/** 商家删除自己的店铺 DELETE /shop/my/{shopId} */
export function deleteMyShop(shopId: string): Promise<void> {
  return delVoid(`${BASE}/my/${shopId}`)
}

/** 管理员分页查询店铺列表 POST /shop/admin/list */
export function getShopListByAdmin(data: ShopApplyQueryInput) {
  return fetchShopPage(`${BASE}/admin/list`, data)
}

/** 管理员查询商家申请列表 POST /shop/admin/apply/query */
export function getShopApplyListByAdmin(data: ShopApplyQueryInput) {
  return fetchShopPage(`${BASE}/admin/apply/query`, data)
}

/** 管理员审批商家申请 */
export function auditShop(data: ShopAuditRequest) {
  return shopAdminVoid(`${BASE}/admin/audit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
}

/** 管理员禁用/启用店铺 POST /shop/admin/status/update */
export function updateShopStatus(params: UpdateShopStatusParams) {
  const body = new URLSearchParams({
    shopId: params.shopId,
    shopStatus: String(params.shopStatus),
  })

  return shopAdminVoid(`${BASE}/admin/status/update`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString(),
  })
}

const SHOP_COLLECT_LONG_FIELDS = ['id', 'shopId']

/** 收藏店铺 POST /shop/collect/add */
export function collectShop(data: ShopCollectRequest): Promise<void> {
  return postVoid(`${BASE}/collect/add`, data)
}

/** 取消收藏店铺 POST /shop/collect/cancel */
export function cancelCollectShop(data: ShopCollectRequest): Promise<void> {
  return postVoid(`${BASE}/collect/cancel`, data)
}

/** 查询店铺收藏状态 GET /shop/collect/status/{shopId} */
export async function getShopCollectStatus(shopId: string): Promise<ShopCollectStatusVO> {
  const response = await request.get(`${BASE}/collect/status/${shopId}`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<ShopCollectStatusRaw>>(
          raw,
          SHOP_COLLECT_LONG_FIELDS,
        )
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<ShopCollectStatusRaw>
  if (res.success === false) {
    throw new Error(res.message || '加载店铺收藏状态失败')
  }
  return normalizeShopCollectStatus(res.data ?? {})
}

/** 查询我的店铺收藏列表 GET /shop/collect/list（需登录） */
export async function getMyShopCollectList(): Promise<ShopCollectVO[]> {
  const response = await request.get(`${BASE}/collect/list`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<ShopCollectRaw[]>>(
          raw,
          SHOP_COLLECT_LONG_FIELDS,
        )
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<ShopCollectRaw[]>
  if (res.success === false) {
    throw new Error(res.message || '加载收藏店铺失败')
  }
  return normalizeShopCollectList(res.data ?? [])
}
