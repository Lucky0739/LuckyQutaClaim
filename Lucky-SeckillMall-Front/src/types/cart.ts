/** 购物车行 ID */
export type CartId = string

export const CART_MAX_GOODS_COUNT = 999

/** 选中状态：0=未选，1=已选 */
export const CartSelected = {
  NO: 0,
  YES: 1,
} as const

export interface AddCartRequest {
  goodsId: string
  skuId: string
  goodsCount: number
}

export interface BatchDeleteCartRequest {
  cartIds: CartId[]
}

export interface CartVORaw {
  id?: number | string
  goodsId?: number | string
  skuId?: number | string
  shopId?: number | string
  shopName?: string
  goodsName?: string
  goodsImg?: string
  skuSpecName?: string
  salePrice?: number | string
  stockNum?: number
  goodsStatus?: number
  auditStatus?: number
  skuStatus?: number
  goodsCount?: number
  selected?: number
  subtotal?: number | string
  valid?: boolean
  invalidReason?: string
  createTime?: string
}

export interface CartVO {
  id: CartId
  goodsId: string
  skuId: string
  shopId?: string
  shopName?: string
  goodsName?: string
  goodsImg?: string
  skuSpecName?: string
  salePrice?: number
  stockNum?: number
  goodsStatus?: number
  auditStatus?: number
  skuStatus?: number
  goodsCount: number
  selected: number
  subtotal?: number
  valid?: boolean
  invalidReason?: string
  createTime?: string
}

export interface CartSummaryVORaw {
  totalLines?: number
  totalQuantity?: number
  selectedLines?: number
  selectedQuantity?: number
  selectedAmount?: number | string
  allSelected?: boolean
}

export interface CartSummaryVO {
  totalLines: number
  totalQuantity: number
  selectedLines: number
  selectedQuantity: number
  selectedAmount: number
  allSelected: boolean
}

export interface CartListVORaw {
  items?: CartVORaw[]
  summary?: CartSummaryVORaw
}

export interface CartListVO {
  items: CartVO[]
  summary: CartSummaryVO
}

function toId(value?: number | string | null): string {
  if (value == null || value === '') return ''
  return String(value)
}

function toNumber(value?: number | string | null): number | undefined {
  if (value == null || value === '') return undefined
  const num = Number(value)
  return Number.isFinite(num) ? num : undefined
}

function normalizeSelected(value?: number | boolean | string | null): number {
  if (value === CartSelected.YES || value === true || value === '1') {
    return CartSelected.YES
  }
  return CartSelected.NO
}

export function normalizeCartItem(raw: CartVORaw): CartVO | null {
  const id = toId(raw.id)
  const goodsId = toId(raw.goodsId)
  const skuId = toId(raw.skuId)
  if (!id || !goodsId || !skuId) return null

  return {
    id,
    goodsId,
    skuId,
    shopId: toId(raw.shopId) || undefined,
    shopName: raw.shopName,
    goodsName: raw.goodsName,
    goodsImg: raw.goodsImg,
    skuSpecName: raw.skuSpecName,
    salePrice: toNumber(raw.salePrice),
    stockNum: raw.stockNum,
    goodsStatus: raw.goodsStatus,
    auditStatus: raw.auditStatus,
    skuStatus: raw.skuStatus,
    goodsCount: Math.max(1, raw.goodsCount ?? 1),
    selected: normalizeSelected(raw.selected),
    subtotal: toNumber(raw.subtotal),
    valid: raw.valid,
    invalidReason: raw.invalidReason,
    createTime: raw.createTime,
  }
}

export function normalizeCartList(raw?: CartListVORaw | null): CartListVO {
  const items = (raw?.items ?? [])
    .map(normalizeCartItem)
    .filter((item): item is CartVO => item != null)
  const summaryRaw = raw?.summary
  return {
    items,
    summary: {
      totalLines: summaryRaw?.totalLines ?? items.length,
      totalQuantity: summaryRaw?.totalQuantity ?? items.reduce((s, i) => s + i.goodsCount, 0),
      selectedLines: summaryRaw?.selectedLines ?? 0,
      selectedQuantity: summaryRaw?.selectedQuantity ?? 0,
      selectedAmount: toNumber(summaryRaw?.selectedAmount) ?? 0,
      allSelected: summaryRaw?.allSelected ?? false,
    },
  }
}

export function isCartItemSelected(item: CartVO): boolean {
  return item.selected === CartSelected.YES
}

export function formatCartAmount(value?: number): string {
  const num = value ?? 0
  return num.toFixed(2)
}

export interface CartShopGroup {
  shopId: string
  shopName: string
  items: CartVO[]
}

export function groupCartByShop(items: CartVO[]): CartShopGroup[] {
  const map = new Map<string, CartShopGroup>()
  for (const item of items) {
    const shopId = item.shopId || 'default'
    const existing = map.get(shopId)
    if (existing) {
      existing.items.push(item)
      continue
    }
    map.set(shopId, {
      shopId,
      shopName: item.shopName || '店铺',
      items: [item],
    })
  }
  return [...map.values()]
}
