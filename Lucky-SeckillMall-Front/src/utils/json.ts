/** 将 JSON 文本中的 Long 类型字段转为字符串，避免 JS 精度丢失 */
export function parseJsonPreservingLongIds<T>(raw: string, fields = ['id', 'userId']): T {
  let fixed = raw
  for (const field of fields) {
    fixed = fixed.replace(new RegExp(`"${field}"\\s*:\\s*(\\d+)`, 'g'), `"${field}":"$1"`)
  }
  return JSON.parse(fixed) as T
}

/** 检测列表是否存在重复 ID（开发期辅助排查） */
export function findDuplicateIds(ids: string[]): string[] {
  const seen = new Set<string>()
  const duplicates = new Set<string>()
  for (const id of ids) {
    if (!id) continue
    if (seen.has(id)) duplicates.add(id)
    seen.add(id)
  }
  return [...duplicates]
}
