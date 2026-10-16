/** 商品分类 ID（后端 Long，前端以字符串保存避免精度丢失） */
export type CategoryId = string

/** 分类状态：0 禁用，1 启用 */
export const CategoryStatus = {
  DISABLED: 0,
  ENABLED: 1,
} as const

/** 商品分类，对应后端 GoodsCategory */
export interface GoodsCategory {
  id: CategoryId
  categoryName: string
  parentId?: CategoryId | null
  sort?: number
  status?: number
  isDelete?: number
  createTime?: string
  updateTime?: string
}

export interface CategoryAddRequest {
  categoryName: string
  parentId?: CategoryId | null
  sort?: number
}

export interface CategoryUpdateRequest extends CategoryAddRequest {
  id: CategoryId
  status?: number
}

/** 是否为顶级（父）分类：后端用 parentId = 0 表示无父级 */
export function isRootCategory(parentId?: CategoryId | null | number): boolean {
  return parentId == null || parentId === '' || parentId === 0 || parentId === '0'
}

/** 规范化 parentId：顶级分类统一为 null */
export function normalizeParentId(parentId?: CategoryId | null | number): CategoryId | null {
  if (isRootCategory(parentId)) return null
  return String(parentId)
}

/** 提交给后端的 parentId：顶级分类传 0 */
export function toApiParentId(parentId?: CategoryId | null): number | string {
  if (isRootCategory(parentId)) return 0
  return parentId as string
}

/** 将接口返回的分类列表规范化 */
export function normalizeCategoryList(list: GoodsCategory[] | null | undefined): GoodsCategory[] {
  if (!Array.isArray(list)) return []
  return list.map((item) => ({
    ...item,
    id: String(item.id),
    parentId: normalizeParentId(item.parentId),
  }))
}

/** 将单条分类记录规范化 */
export function normalizeCategory(item: GoodsCategory): GoodsCategory {
  return {
    ...item,
    id: String(item.id),
    parentId: normalizeParentId(item.parentId),
  }
}
