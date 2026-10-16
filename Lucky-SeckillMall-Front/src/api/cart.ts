import request, { delVoid, postVoid, putVoid } from '@/utils/request'
import type { ApiResponse } from '@/types/common'
import { isNotLoginResponse } from '@/types/common'
import type {
  AddCartRequest,
  BatchDeleteCartRequest,
  CartId,
  CartListVO,
  CartListVORaw,
} from '@/types/cart'
import { normalizeCartList } from '@/types/cart'
import { parseJsonPreservingLongIds } from '@/utils/json'

const BASE = '/cart'

const CART_LONG_ID_FIELDS = [
  'id',
  'goodsId',
  'skuId',
  'shopId',
  'cartId',
  'userId',
]

function parseCartListResponse(raw: string): ApiResponse<CartListVORaw> {
  return parseJsonPreservingLongIds<ApiResponse<CartListVORaw>>(raw, CART_LONG_ID_FIELDS)
}

function assertCartSuccess<T>(res: ApiResponse<T>, fallbackMessage: string): T | undefined {
  if (res.success === false) {
    throw new Error(res.message || fallbackMessage)
  }
  return res.data
}

/** 添加商品到购物车 POST /cart/add */
export function addCart(data: AddCartRequest): Promise<void> {
  return postVoid(`${BASE}/add`, {
    goodsId: data.goodsId,
    skuId: data.skuId,
    goodsCount: data.goodsCount,
  })
}

/** 查询购物车列表 GET /cart/list */
export async function getCartList(): Promise<CartListVO> {
  const response = await request.get(`${BASE}/list`, {
    transformResponse: [(raw: string | ApiResponse<CartListVORaw>) => {
      if (typeof raw === 'string') {
        if (!raw) return raw
        try {
          return parseCartListResponse(raw)
        } catch {
          return JSON.parse(raw) as ApiResponse<CartListVORaw>
        }
      }
      return raw
    }],
  })
  const res = response.data as ApiResponse<CartListVORaw>
  const data = assertCartSuccess(res, '加载购物车失败')
  return normalizeCartList(data)
}

/** 查询购物车商品总件数 GET /cart/count（未登录时后端返回 0） */
export async function getCartCount(): Promise<number> {
  const response = await request.get(`${BASE}/count`)
  const res = response.data as ApiResponse<number>
  if (isNotLoginResponse(res)) {
    return 0
  }
  if (res.success === false) {
    throw new Error(res.message || '获取购物车数量失败')
  }
  return Number(res.data ?? 0)
}

/** 删除购物车商品 DELETE /cart/delete/{cartId} */
export function deleteCart(cartId: CartId): Promise<void> {
  return delVoid(`${BASE}/delete/${cartId}`)
}

/** 更新购物车商品数量 PUT /cart/updateQuantity/{cartId} */
export function updateCartQuantity(cartId: CartId, goodsCount: number): Promise<void> {
  return putVoid(`${BASE}/updateQuantity/${cartId}`, undefined, {
    params: { goodsCount },
  })
}

/** 选中/取消选中 PUT /cart/select/{cartId} */
export function selectCartItem(cartId: CartId, selected: number): Promise<void> {
  return putVoid(`${BASE}/select/${cartId}`, undefined, {
    params: { selected },
  })
}

/** 全选/取消全选 PUT /cart/selectAll */
export function selectAllCart(selected: number): Promise<void> {
  return putVoid(`${BASE}/selectAll`, undefined, {
    params: { selected },
  })
}

/** 批量删除 POST /cart/batchDelete */
export function batchDeleteCart(cartIds: CartId[]): Promise<void> {
  const payload: BatchDeleteCartRequest = { cartIds }
  return postVoid(`${BASE}/batchDelete`, payload)
}

/** 批量删除 DELETE /cart/batchDelete?cartIds= */
export function batchDeleteCartByQuery(cartIds: CartId[]): Promise<void> {
  return delVoid(`${BASE}/batchDelete`, {
    params: { cartIds },
    paramsSerializer: {
      indexes: null,
    },
  })
}

/** 删除已选 DELETE /cart/deleteSelected */
export function deleteSelectedCart(): Promise<void> {
  return delVoid(`${BASE}/deleteSelected`)
}

/** 清空购物车 DELETE /cart/clear */
export function clearCart(): Promise<void> {
  return delVoid(`${BASE}/clear`)
}
