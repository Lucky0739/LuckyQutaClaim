import request, { delVoid, postForm, postVoid, putVoid } from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/common'
import type { UploadPictureResult } from '@/types/common'
import type {
  AddGoodsRequest,
  GoodsAuditRequest,
  GoodsCollectRaw,
  GoodsCollectRequest,
  GoodsCollectStatusRaw,
  GoodsCollectStatusVO,
  GoodsCollectVO,
  GoodsDetailRaw,
  GoodsDetailVO,
  GoodsHomeQueryRequest,
  GoodsId,
  GoodsListRaw,
  GoodsListVO,
  GoodsQueryRequest,
  SkuImgRaw,
  SkuImgVO,
  UpdateGoodsRequest,
} from '@/types/goods'
import {
  buildHomeGoodsQuery,
  buildHomeGoodsRankFallbackQuery,
  normalizeHomeSortType,
} from '@/types/goods'
import {
  normalizeGoodsCollectList,
  normalizeGoodsCollectStatus,
  normalizeGoodsDetail,
  normalizeGoodsList,
  normalizeSkuImg,
  type ShopGoodsQueryParams,
} from '@/types/goods'
import { parseJsonPreservingLongIds } from '@/utils/json'

const BASE = '/goods'

function buildGoodsPageQueryParams(data: GoodsQueryRequest): Record<string, string | number> {
  const params: Record<string, string | number> = {
    pageNum: data.pageNum,
    pageSize: data.pageSize,
  }
  if (data.goodsName) params.goodsName = data.goodsName
  if (data.status != null) params.status = data.status
  if (data.auditStatus != null) params.auditStatus = data.auditStatus
  if (data.categoryId) params.categoryId = data.categoryId
  if (data.parentCategoryId) params.parentCategoryId = data.parentCategoryId
  if (data.shopId) params.shopId = data.shopId
  return params
}

function parseGoodsPageResponse(
  res: ApiResponse<PageResult<GoodsListRaw>>,
  data: GoodsQueryRequest,
): PageResult<GoodsListVO> {
  if (res.success === false) {
    throw new Error(res.message || '加载商品列表失败')
  }

  const page = res.data ?? {
    records: [],
    total: 0,
    pageNum: data.pageNum,
    pageSize: data.pageSize,
    totalPages: 0,
  }

  return {
    ...page,
    total: Number(page.total ?? 0),
    pageNum: Number(page.pageNum ?? data.pageNum),
    pageSize: Number(page.pageSize ?? data.pageSize),
    totalPages: Number(page.totalPages ?? 0),
    records: normalizeGoodsList(page.records ?? []),
  }
}

/** 分页查询商品列表 GET /goods/page（支持 parentCategoryId / categoryId 等） */
export async function getGoodsPage(data: GoodsQueryRequest) {
  const response = await request.get(`${BASE}/page`, {
    params: buildGoodsPageQueryParams(data),
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<GoodsListRaw>>>(
          raw,
          [...HOME_GOODS_LONG_ID_FIELDS, 'shopId'],
        )
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  return parseGoodsPageResponse(response.data as ApiResponse<PageResult<GoodsListRaw>>, data)
}

/** 商品搜索 GET /goods/search（ES 不可用时后端自动降级 MySQL） */
export async function searchGoods(keyword: string, pageNum: number, pageSize: number) {
  const query: GoodsQueryRequest = {
    goodsName: keyword.trim(),
    pageNum,
    pageSize,
  }
  const response = await request.get(`${BASE}/search`, {
    params: {
      keyword: query.goodsName,
      pageNum,
      pageSize,
    },
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<GoodsListRaw>>>(
          raw,
          [...HOME_GOODS_LONG_ID_FIELDS, 'shopId'],
        )
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  return parseGoodsPageResponse(response.data as ApiResponse<PageResult<GoodsListRaw>>, query)
}

const HOME_GOODS_LONG_ID_FIELDS = ['id', 'goodsId', 'categoryId', 'parentCategoryId']

/**
 * Redis 排行与 DB 可见商品不一致时需回退 /goods/page：
 * - 无记录；或
 * - 首页第一页 total 大于实际返回条数（排行里有 ID，但 DB 未上架/未过审被过滤）
 */
function shouldFallbackHomeRankPage(page: PageResult<GoodsListVO>, pageNum: number): boolean {
  if (pageNum !== 1) return false
  const records = page.records ?? []
  if (!records.length) return true
  const total = Number(page.total ?? 0)
  return total > records.length
}

/** 首页商品排行列表 GET /goods/home/list（sortType: weight / collect / sales） */
export async function getHomeGoodsList(data: GoodsHomeQueryRequest) {
  const pageNum = data.pageNum
  const pageSize = data.pageSize
  const response = await request.get(`${BASE}/home/list`, {
    params: {
      sortType: normalizeHomeSortType(data.sortType),
      pageNum,
      pageSize,
    },
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<GoodsListRaw>>>(
          raw,
          HOME_GOODS_LONG_ID_FIELDS,
        )
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const page = parseGoodsPageResponse(
    response.data as ApiResponse<PageResult<GoodsListRaw>>,
    { pageNum, pageSize },
  )

  // Redis 排行未就绪或与 DB 不一致时，回退到 /goods/page 拉全量可见商品
  if (!shouldFallbackHomeRankPage(page, pageNum)) {
    return page
  }

  const auditedPage = await getGoodsPage(buildHomeGoodsRankFallbackQuery(pageNum, pageSize))
  if (auditedPage.records?.length) {
    return auditedPage
  }

  return getGoodsPage(buildHomeGoodsQuery(pageNum, pageSize))
}

/** 分页查询商品列表（兼容旧调用，后端仅提供 GET /goods/page） */
export async function postGoodsPage(data: GoodsQueryRequest) {
  return getGoodsPage(data)
}

/** 按店铺 ID 分页查询商品（走 GET /goods/page?shopId=，与 /goods/shop/{shopId} 等价且更稳定） */
export async function getGoodsPageByShopId(shopId: string, params: ShopGoodsQueryParams) {
  const id = shopId.trim()
  if (!id) {
    throw new Error('店铺 ID 不能为空')
  }

  return getGoodsPage({
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    shopId: id,
    status: params.status,
    auditStatus: params.auditStatus,
  })
}

/** 查看商品详情 GET /goods/detail/{goodsId} */
export async function getGoodsDetail(goodsId: GoodsId): Promise<GoodsDetailVO> {
  const response = await request.get(`${BASE}/detail/${goodsId}`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<GoodsDetailRaw>>(raw, [
          'id',
          'goodsId',
          'shopId',
          'categoryId',
          'skuId',
        ])
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<GoodsDetailRaw>
  if (res.success === false) {
    throw new Error(res.message || '加载商品详情失败')
  }
  if (!res.data) {
    throw new Error('商品不存在')
  }
  return normalizeGoodsDetail(res.data)
}

/** 查询 SKU 信息并读取规格图片 GET /goods/sku/{skuId} */
export async function getSkuImage(skuId: string): Promise<SkuImgVO> {
  const response = await request.get(`${BASE}/sku/${skuId}`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<SkuImgRaw>>(raw, ['skuId'])
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<SkuImgRaw>
  if (res.success === false) {
    throw new Error(res.message || '加载规格图片失败')
  }
  if (!res.data) {
    throw new Error('规格图片不存在')
  }
  return normalizeSkuImg(res.data)
}

/** 收藏商品 POST /goods/collect/add */
export function collectGoods(data: GoodsCollectRequest): Promise<void> {
  return postVoid('/goods/collect/add', data)
}

/** 取消收藏商品 POST /goods/collect/cancel */
export function cancelCollectGoods(data: GoodsCollectRequest): Promise<void> {
  return postVoid('/goods/collect/cancel', data)
}

/** 查询商品收藏状态 GET /goods/collect/status/{goodsId} */
export async function getGoodsCollectStatus(goodsId: GoodsId): Promise<GoodsCollectStatusVO> {
  const response = await request.get(`/goods/collect/status/${goodsId}`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<GoodsCollectStatusRaw>>(raw, ['goodsId'])
      } catch {
        return JSON.parse(raw)
      }
    }],
  })
  const res = response.data as ApiResponse<GoodsCollectStatusRaw>
  if (res.success === false) {
    throw new Error(res.message || '加载收藏状态失败')
  }
  return normalizeGoodsCollectStatus(res.data ?? {})
}

/** 查询我的商品收藏列表 GET /goods/collect/list（需登录） */
export async function getMyGoodsCollectList(): Promise<GoodsCollectVO[]> {
  const response = await request.get(`${BASE}/collect/list`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<GoodsCollectRaw[]>>(raw, [
          'id',
          'goodsId',
        ])
      } catch {
        return JSON.parse(raw)
      }
    }],
  })
  const res = response.data as ApiResponse<GoodsCollectRaw[]>
  if (res.success === false) {
    throw new Error(res.message || '加载收藏商品失败')
  }
  return normalizeGoodsCollectList(res.data ?? [])
}

/** 商户申请上架商品 POST /goods/applyOnShelf */
export function applyOnShelf(goodsId: GoodsId): Promise<void> {
  return postVoid(`${BASE}/applyOnShelf`, null, { params: { goodsId } })
}

/** 下架商品 POST /goods/offShelf/{goodsId} */
export function offShelf(goodsId: GoodsId): Promise<void> {
  return postVoid(`${BASE}/offShelf/${goodsId}`)
}

/** 新增商品 POST /goods/add */
export async function addGoods(data: AddGoodsRequest): Promise<GoodsId> {
  const response = await request.post(`${BASE}/add`, data, {
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
    throw new Error(res.message || '发布商品失败')
  }

  return String(res.data ?? '')
}

/** 上传商品图片 POST /goods/img/upload */
export function uploadGoodsImg(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return postForm<UploadPictureResult>(`${BASE}/img/upload`, formData)
}

const ADMIN_BASE = `${BASE}/admin`

const ADMIN_GOODS_LONG_ID_FIELDS = ['id', 'goodsId', 'shopId', 'categoryId', 'parentCategoryId']

async function postAdminGoodsPage(url: string, data: GoodsQueryRequest) {
  const response = await request.post(url, data, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<GoodsListRaw>>>(
          raw,
          ADMIN_GOODS_LONG_ID_FIELDS,
        )
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  return parseGoodsPageResponse(response.data as ApiResponse<PageResult<GoodsListRaw>>, data)
}

/** 管理员查询待审核商品 POST /goods/admin/pendingAudit/page（服务端固定 audit_status=1，直查 DB） */
export function getPendingAuditGoodsPage(data: GoodsQueryRequest) {
  return postAdminGoodsPage(`${ADMIN_BASE}/pendingAudit/page`, data)
}

/** 管理员查询已审核商品 POST /goods/admin/audited/page */
export function getAuditedGoodsPage(data: GoodsQueryRequest) {
  return postAdminGoodsPage(`${ADMIN_BASE}/audited/page`, data)
}

/** 管理员审核商品 POST /goods/admin/audit */
export function auditGoods(data: GoodsAuditRequest) {
  return postVoid(`${ADMIN_BASE}/audit`, {
    goodsId: data.goodsId,
    auditStatus: data.auditStatus,
    auditRemark: data.auditRemark?.trim() || undefined,
  })
}

const MY_BASE = `${BASE}/my`

const MY_GOODS_LONG_ID_FIELDS = ['id', 'goodsId', 'categoryId', 'parentCategoryId', 'shopId', 'skuId']

/** 商户分页查询我发布的商品 GET /goods/my/page */
export async function getMyGoodsPage(data: GoodsQueryRequest) {
  const response = await request.get(`${MY_BASE}/page`, {
    params: buildGoodsPageQueryParams(data),
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<PageResult<GoodsListRaw>>>(
          raw,
          MY_GOODS_LONG_ID_FIELDS,
        )
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  return parseGoodsPageResponse(response.data as ApiResponse<PageResult<GoodsListRaw>>, data)
}

/** 商户查看我发布的商品详情 GET /goods/my/detail/{goodsId} */
export async function getMyGoodsDetail(goodsId: GoodsId): Promise<GoodsDetailVO> {
  const response = await request.get(`${MY_BASE}/detail/${goodsId}`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<GoodsDetailRaw>>(raw, MY_GOODS_LONG_ID_FIELDS)
      } catch {
        return JSON.parse(raw)
      }
    }],
  })

  const res = response.data as ApiResponse<GoodsDetailRaw>
  if (res.success === false) {
    throw new Error(res.message || '加载商品详情失败')
  }
  if (!res.data) {
    throw new Error('商品不存在')
  }
  return normalizeGoodsDetail(res.data)
}

/** 商户更新我发布的商品 PUT /goods/my/update */
export function updateMyGoods(data: UpdateGoodsRequest): Promise<void> {
  return putVoid(`${MY_BASE}/update`, data)
}

/** 商户删除我发布的商品 DELETE /goods/my/{goodsId} */
export function deleteMyGoods(goodsId: GoodsId): Promise<void> {
  return delVoid(`${MY_BASE}/${goodsId}`)
}
