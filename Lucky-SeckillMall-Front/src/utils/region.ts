import { codeToText, regionData } from 'element-china-area-data'
import type { DataItem } from 'element-china-area-data'

export { regionData }

/** 区域码转省市区名称 */
export function codesToRegion(codes: string[]): { province: string; city: string; area: string } {
  if (codes.length !== 3) {
    return { province: '', city: '', area: '' }
  }

  return {
    province: codeToText[codes[0]] ?? '',
    city: codeToText[codes[1]] ?? '',
    area: codeToText[codes[2]] ?? '',
  }
}

/** 省市区名称转区域码（用于编辑回显） */
export function regionToCodes(province: string, city: string, area: string): string[] {
  if (!province || !city || !area) return []

  for (const p of regionData as DataItem[]) {
    if (p.label !== province) continue

    for (const c of p.children ?? []) {
      if (c.label !== city) continue

      for (const a of c.children ?? []) {
        if (a.label === area) {
          return [p.value, c.value, a.value]
        }
      }
    }
  }

  return []
}

/** 已选地区展示文案 */
export function formatRegionText(province: string, city: string, area: string): string {
  if (!province && !city && !area) return ''
  return [province, city, area].filter(Boolean).join(' / ')
}
