import { getCategoryChildren, getParentCategories } from '@/api/category'
import type { GoodsCategorySelection } from '@/types/goods'
import type { LocationQuery, RouteLocationRaw } from 'vue-router'

/** 从路由 query 读取单个字符串（兼容 string[] / number） */
export function parseRouteQueryString(value: unknown): string | undefined {
  if (typeof value === 'string') {
    const trimmed = value.trim()
    return trimmed || undefined
  }
  if (Array.isArray(value)) {
    return parseRouteQueryString(value[0])
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return String(value)
  }
  return undefined
}

/** 从路由 query 解析分类筛选（/category） */
export function parseCategoryFilterFromRoute(
  query: LocationQuery,
): GoodsCategorySelection | null {
  const parentCategoryId = parseRouteQueryString(query.parentCategoryId)
  const categoryId = parseRouteQueryString(query.categoryId)
  if (!parentCategoryId && !categoryId) return null

  return {
    parentCategoryId,
    categoryId,
    parentName: parseRouteQueryString(query.parentName),
    categoryName: parseRouteQueryString(query.categoryName),
  }
}

/** 首页 iPhone 促销位对应的子分类名称 */
export const IPHONE_PROMO_CATEGORY_CHILD_NAME = '苹果手机'

/** 首页 618 促销位对应的分类名称（子分类或父分类） */
export const TABLET_PROMO_CATEGORY_NAME = '平板'

/** 首页运动户外促销位对应的分类名称（子分类或父分类） */
export const SPORTS_OUTDOOR_PROMO_CATEGORY_NAME = '户外装备'

/**
 * 按分类名称解析筛选条件（先匹配父分类，再匹配子分类）
 */
export async function resolveCategoryByName(
  categoryName: string,
): Promise<GoodsCategorySelection | null> {
  const target = categoryName.trim()
  if (!target) return null

  const parents = await getParentCategories()
  const parentMatch = parents.find((item) => item.categoryName?.trim() === target)
  if (parentMatch) {
    return {
      parentCategoryId: parentMatch.id,
      parentName: parentMatch.categoryName,
    }
  }

  for (const parent of parents) {
    const children = await getCategoryChildren(parent.id)
    const child = children.find((item) => item.categoryName?.trim() === target)
    if (child) {
      return {
        parentCategoryId: parent.id,
        parentName: parent.categoryName,
        categoryId: child.id,
        categoryName: child.categoryName,
      }
    }
  }
  return null
}

/** @deprecated 使用 resolveCategoryByName */
export const resolveCategoryByChildName = resolveCategoryByName

/** 构建分类商品专页路由（/category） */
export function buildCategoryGoodsRoute(filter: GoodsCategorySelection): RouteLocationRaw {
  const query: Record<string, string> = {}
  if (filter.parentCategoryId) query.parentCategoryId = filter.parentCategoryId
  if (filter.categoryId) query.categoryId = filter.categoryId
  if (filter.parentName) query.parentName = filter.parentName
  if (filter.categoryName) query.categoryName = filter.categoryName
  return { path: '/category', query }
}

/** @deprecated 使用 buildCategoryGoodsRoute */
export const buildCategorySearchRoute = buildCategoryGoodsRoute
