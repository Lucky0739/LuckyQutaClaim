import request, { postVoid } from '@/utils/request'
import type { ApiResponse } from '@/types/common'
import {
  normalizeAddressItem,
  normalizeAddressList,
  type AddressId,
  type UserAddressRaw,
  type UserAddressAddRequest,
  type UserAddressUpdateRequest,
  type UserAddressVO,
} from '@/types/address'

const BASE = '/user/address'

/**
 * 解析地址列表 JSON，将 Long 类型的 id 保留为字符串，避免精度丢失
 */
function parseAddressListResponse(raw: string): ApiResponse<UserAddressVO[]> {
  const fixed = raw.replace(/"id"\s*:\s*(\d+)/g, '"id":"$1"')
  return JSON.parse(fixed) as ApiResponse<UserAddressVO[]>
}

function parseAddressResponse(raw: string): ApiResponse<UserAddressRaw> {
  const fixed = raw.replace(/"id"\s*:\s*(\d+)/g, '"id":"$1"')
  return JSON.parse(fixed) as ApiResponse<UserAddressRaw>
}

/**
 * 查询用户地址列表
 * GET /user/address/list
 * @returns 当前登录用户的收货地址列表（id 即后续删除/修改所用的地址 ID）
 */
export async function listAddress() {
  const response = await request.get(`${BASE}/list`, {
    transformResponse: [(data: string) => {
      if (typeof data !== 'string' || !data) return data
      try {
        return parseAddressListResponse(data)
      } catch {
        return JSON.parse(data)
      }
    }],
  })

  const res = response.data as ApiResponse<UserAddressVO[]>
  if (res.success === false) {
    throw new Error(res.message || '加载地址列表失败')
  }

  return normalizeAddressList(res.data)
}

/**
 * 根据地址 ID 查询收货地址
 * GET /user/address/get/{userAddressId}
 */
export async function getAddressById(userAddressId: AddressId) {
  const response = await request.get(`${BASE}/get/${userAddressId}`, {
    transformResponse: [(data: string) => {
      if (typeof data !== 'string' || !data) return data
      try {
        return parseAddressResponse(data)
      } catch {
        return JSON.parse(data)
      }
    }],
  })

  const res = response.data as ApiResponse<UserAddressRaw>
  if (res.success === false) {
    throw new Error(res.message || '加载地址详情失败')
  }
  if (!res.data) {
    throw new Error('地址不存在')
  }
  return normalizeAddressItem(res.data)
}

/**
 * 新增收货地址
 * POST /user/address/add
 */
export function addAddress(data: UserAddressAddRequest) {
  return postVoid(`${BASE}/add`, data)
}

/**
 * 修改收货地址
 * POST /user/address/update
 * @param data.id 地址 ID（与列表 UserAddressVO.id 相同）
 */
export function updateAddress(data: UserAddressUpdateRequest) {
  return postVoid(`${BASE}/update`, data)
}

/**
 * 删除收货地址
 * POST /user/address/delete/{userAddressId}
 * @param userAddressId 地址 ID（与列表 UserAddressVO.id 为同一 Long 字段）
 */
export function deleteAddress(userAddressId: AddressId) {
  return postVoid(`${BASE}/delete/${userAddressId}`)
}
