/** 背景图展示项（由 URL 列表转换，id 与 bannerUrl 相同） */
export interface BannerVO {
  id: string
  bannerUrl: string
  sort: number
}

/** 保存轮播图请求，对应后端 BannerRequest */
export interface BannerRequest {
  bannerUrl: string
  sort: number
}

/** 更新排序请求，对应后端 BannerSortRequest */
export interface BannerSortRequest {
  bannerId: string
  sort: number
}

export function normalizeBannerUrlList(urls: string[] | null | undefined): BannerVO[] {
  return (urls ?? [])
    .map((url) => url?.trim())
    .filter((url): url is string => !!url)
    .map((bannerUrl, index) => ({
      id: bannerUrl,
      bannerUrl,
      sort: index,
    }))
}

export function buildBannerSaveRequests(banners: BannerVO[]): BannerRequest[] {
  return banners.map((item, index) => ({
    bannerUrl: item.bannerUrl,
    sort: index,
  }))
}
