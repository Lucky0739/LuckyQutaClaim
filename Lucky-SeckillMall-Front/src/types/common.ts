/** 统一响应结构，对应后端 Response<T> */
export interface ApiResponse<T = unknown> {
  success: boolean
  message?: string
  errorCode?: string
  data?: T
}

/** 未登录业务码（Sa-Token NotLoginException → Response.fail） */
export const USER_NOT_LOGIN_CODE = '20014'

export function isNotLoginResponse(res?: ApiResponse | null): boolean {
  return res?.errorCode === USER_NOT_LOGIN_CODE
}

/** 分页结果，对应后端 PageResult<T> */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

/** 图片上传结果，对应后端 UploadPictureResult */
export interface UploadPictureResult {
  imageUrl: string
  thumbnailUrl?: string
  picSize?: number
  picWidth?: number
  picHeight?: number
  picScale?: number
  picFormat?: string
  picColor?: string
}
