export interface AdminNavItem {
  label: string
  routeName: string
}

export const ADMIN_NAV_ITEMS: AdminNavItem[] = [
  { label: '用户管理', routeName: 'AdminUsers' },
  { label: '背景图管理', routeName: 'AdminBanners' },
  { label: '店铺管理', routeName: 'AdminShops' },
  { label: '秒杀活动', routeName: 'AdminActivities' },
  { label: '秒杀商品审核', routeName: 'AdminSeckillGoods' },
  { label: '商品管理', routeName: 'AdminGoods' },
  { label: '分类管理', routeName: 'AdminCategories' },
  { label: '失败订单处理', routeName: 'AdminFailedOrders' },
]
