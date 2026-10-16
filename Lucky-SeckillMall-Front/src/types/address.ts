import { MOBILE_PATTERN } from '@/types/user'

/** 地址 ID（后端 Long，列表字段 id = 删除路径参数 userAddressId） */
export type AddressId = string

/** 是否默认地址：0=否，1=是 */
export const AddressDefault = {
  NO: 0,
  YES: 1,
} as const

/** 用户收货地址（对应后端 UserAddressVO） */
export interface UserAddressVO {
  /** 地址 ID（Long，与 DELETE /user/address/delete/{userAddressId} 的路径参数为同一字段） */
  id: AddressId
  /** 收货人姓名 */
  receiver: string
  /** 收货人手机号 */
  mobile: string
  /** 省份 */
  province: string
  /** 城市 */
  city: string
  /** 区县 */
  area: string
  /** 详细地址 */
  detailAddress: string
  /** 是否默认：0=否，1=是 */
  isDefault: number
}

export type UserAddressRaw = Omit<UserAddressVO, 'id'> & {
  id: string | number
}

/** 新增收货地址（对应后端 UserAddressAddRequest） */
export interface UserAddressAddRequest {
  receiver: string
  mobile: string
  province: string
  city: string
  area: string
  detailAddress: string
  /** 是否默认：0=否，1=是 */
  isDefault?: number
}

/** 修改收货地址（对应后端 UserAddressUpdateRequest） */
export interface UserAddressUpdateRequest {
  /** 地址 ID（Long，不能为空） */
  id: AddressId
  receiver: string
  mobile: string
  province: string
  city: string
  area: string
  detailAddress: string
  /** 是否默认：0=否，1=是 */
  isDefault?: number
}

export interface AddressFormModel {
  receiver: string
  mobile: string
  province: string
  city: string
  area: string
  detailAddress: string
  isDefault: number
}

export interface AddressFormErrors {
  receiver?: string
  mobile?: string
  region?: string
  detailAddress?: string
}

export function createEmptyAddressForm(): AddressFormModel {
  return {
    receiver: '',
    mobile: '',
    province: '',
    city: '',
    area: '',
    detailAddress: '',
    isDefault: AddressDefault.NO,
  }
}

/** 统一转为地址 ID 字符串，避免 Long 精度丢失 */
export function toAddressId(id: string | number): AddressId {
  return String(id)
}

/** 规范化单条地址（确保 id 为字符串） */
export function normalizeAddressItem(item: UserAddressRaw): UserAddressVO {
  return {
    ...item,
    id: toAddressId(item.id),
  }
}

/** 校验并组装新增地址请求体 */
export function buildAddressPayload(form: AddressFormModel): {
  payload: UserAddressAddRequest | null
  errors: AddressFormErrors
} {
  const errors: AddressFormErrors = {}
  const receiver = form.receiver.trim()
  const mobile = form.mobile.trim()
  const province = form.province.trim()
  const city = form.city.trim()
  const area = form.area.trim()
  const detailAddress = form.detailAddress.trim()

  if (!receiver) errors.receiver = '收货人姓名不能为空'
  if (!mobile) errors.mobile = '手机号不能为空'
  else if (!MOBILE_PATTERN.test(mobile)) errors.mobile = '手机号格式错误'
  if (!province || !city || !area) errors.region = '请选择所在地区'
  if (!detailAddress) errors.detailAddress = '详细地址不能为空'

  if (Object.keys(errors).length > 0) {
    return { payload: null, errors }
  }

  return {
    payload: {
      receiver,
      mobile,
      province,
      city,
      area,
      detailAddress,
      isDefault: form.isDefault === AddressDefault.YES ? AddressDefault.YES : AddressDefault.NO,
    },
    errors,
  }
}

/** 校验并组装修改地址请求体 */
export function buildAddressUpdatePayload(
  form: AddressFormModel,
  id: AddressId,
): {
  payload: UserAddressUpdateRequest | null
  errors: AddressFormErrors
} {
  const { payload, errors } = buildAddressPayload(form)
  if (!payload) return { payload: null, errors }

  return {
    payload: { ...payload, id },
    errors,
  }
}

/** 判断表单内容是否与原始数据一致 */
export function isSameAddressForm(current: AddressFormModel, original: AddressFormModel): boolean {
  return (
    current.receiver === original.receiver &&
    current.mobile === original.mobile &&
    current.province === original.province &&
    current.city === original.city &&
    current.area === original.area &&
    current.detailAddress === original.detailAddress &&
    current.isDefault === original.isDefault
  )
}

export function cloneAddressForm(form: AddressFormModel): AddressFormModel {
  return { ...form }
}

/** 是否为默认地址 */
export function isDefaultAddress(item: Pick<UserAddressVO, 'isDefault'>) {
  return item.isDefault === AddressDefault.YES
}

/** 地址列表排序：默认地址优先，其余保持原顺序 */
export function sortAddressList(list: UserAddressVO[]): UserAddressVO[] {
  return [...list].sort((a, b) => {
    if (isDefaultAddress(a) && !isDefaultAddress(b)) return -1
    if (!isDefaultAddress(a) && isDefaultAddress(b)) return 1
    return 0
  })
}

/** 规范化地址列表响应 */
export function normalizeAddressList(list: UserAddressRaw[] | null | undefined): UserAddressVO[] {
  if (!Array.isArray(list)) return []
  return sortAddressList(list.map(normalizeAddressItem))
}

export function formatFullAddress(item: Pick<UserAddressVO, 'province' | 'city' | 'area' | 'detailAddress'>) {
  return `${item.province} ${item.city} ${item.area} ${item.detailAddress}`
}

export function maskAddressMobile(mobile: string) {
  if (mobile.length === 11) {
    return `${mobile.slice(0, 3)}****${mobile.slice(7)}`
  }
  return mobile
}
