import request, { delVoid, postVoid, putVoid } from '@/utils/request'
import type { ApiResponse } from '@/types/common'
import {
  normalizeCategory,
  normalizeCategoryList,
  toApiParentId,
  type CategoryAddRequest,
  type CategoryId,
  type CategoryUpdateRequest,
  type GoodsCategory,
} from '@/types/category'
import { parseJsonPreservingLongIds } from '@/utils/json'

const BASE = '/category'

/** 解析分类列表 JSON，将 Long 类型的 id / parentId 保留为字符串 */
function parseCategoryListResponse(raw: string): ApiResponse<GoodsCategory[]> {
  const fixed = raw
    .replace(/"id"\s*:\s*(\d+)/g, '"id":"$1"')
    .replace(/"parentId"\s*:\s*(\d+)/g, '"parentId":"$1"')
  return JSON.parse(fixed) as ApiResponse<GoodsCategory[]>
}

async function fetchCategoryList(url: string) {
  const response = await request.get(url, {
    transformResponse: [(data: string) => {
      if (typeof data !== 'string' || !data) return data
      try {
        return parseCategoryListResponse(data)
      } catch {
        return JSON.parse(data)
      }
    }],
  })

  const res = response.data as ApiResponse<GoodsCategory[]>
  if (res.success === false) {
    throw new Error(res.message || '加载分类失败')
  }

  return normalizeCategoryList(res.data)
}

/** 查询父分类列表 GET /category/parents */
export function getParentCategories() {
  return fetchCategoryList(`${BASE}/parents`)
}

/** 查询子分类 GET /category/children/{parentId} */
export function getCategoryChildren(parentId: CategoryId) {
  return fetchCategoryList(`${BASE}/children/${parentId}`)
}

/** 查询商品分类详情 GET /category/{id} */
export async function getCategoryById(id: CategoryId) {
  const response = await request.get(`${BASE}/${id}`, {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        const fixed = raw
          .replace(/"id"\s*:\s*(\d+)/g, '"id":"$1"')
          .replace(/"parentId"\s*:\s*(\d+)/g, '"parentId":"$1"')
        return JSON.parse(fixed) as ApiResponse<GoodsCategory>
      } catch {
        return JSON.parse(raw)
      }
    }],
  })
  const res = response.data as ApiResponse<GoodsCategory>
  if (res.success === false) {
    throw new Error(res.message || '查询分类详情失败')
  }
  const item = res.data
  if (!item) {
    throw new Error('分类不存在')
  }
  return {
    ...normalizeCategory(item),
  }
}

function toCategoryPayload(data: CategoryAddRequest | CategoryUpdateRequest) {
  return {
    ...data,
    parentId: toApiParentId(data.parentId),
  }
}

/** 查询所有商品分类 GET /category/list */
export function getAllCategories() {
  return fetchCategoryList(`${BASE}/list`)
}

/** 新增商品分类 POST /category/add */
export async function addCategory(data: CategoryAddRequest): Promise<CategoryId> {
  const response = await request.post(`${BASE}/add`, toCategoryPayload(data), {
    transformResponse: [(raw: string) => {
      if (typeof raw !== 'string' || !raw) return raw
      try {
        return parseJsonPreservingLongIds<ApiResponse<string | number>>(raw, ['data'])
      } catch {
        return JSON.parse(raw)
      }
    }],
  })
  const res = response.data as ApiResponse<string | number>
  if (res.success === false) throw new Error(res.message || '新增分类失败')
  return String(res.data ?? '')
}

/** 更新商品分类 PUT /category/update */
export function updateCategory(data: CategoryUpdateRequest) {
  return putVoid(`${BASE}/update`, toCategoryPayload(data))
}

/** 删除商品分类 DELETE /category/delete/{id} */
export function deleteCategory(id: CategoryId) {
  return delVoid(`${BASE}/delete/${id}`)
}

/** 启用商品分类 POST /category/enable/{id} */
export function enableCategory(id: CategoryId) {
  return postVoid(`${BASE}/enable/${id}`)
}

/** 禁用商品分类 POST /category/disable/{id} */
export function disableCategory(id: CategoryId) {
  return postVoid(`${BASE}/disable/${id}`)
}
