import { delVoid, get, post, postForm, postVoid } from '@/utils/request'
import type { UploadPictureResult } from '@/types/common'
import type { BannerRequest, BannerSortRequest, BannerVO } from '@/types/banner'

const BASE = '/banner'

/** 查询轮播图 URL 列表 GET /banner/list */
export function getBannerList() {
  return get<string[]>(`${BASE}/list`)
}

/** 上传单张背景图 POST /banner/img/upload */
export function uploadBannerImg(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return postForm<UploadPictureResult>(`${BASE}/img/upload`, formData)
}

/** 保存背景图 URL 数组 POST /banner/img/save-urls */
export function saveBannerImgUrls(data: BannerRequest[]) {
  return post<string[]>(`${BASE}/img/save-urls`, data)
}

/** 删除单个轮播图 DELETE /banner/{bannerId} */
export function deleteBanner(bannerId: string) {
  return delVoid(`${BASE}/${bannerId}`)
}

/** 更新轮播图排序 POST /banner/sort/update */
export function updateBannerSort(data: BannerSortRequest[]) {
  return postVoid(`${BASE}/sort/update`, data)
}

export type { BannerVO, BannerRequest, BannerSortRequest }
