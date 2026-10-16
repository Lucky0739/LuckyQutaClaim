/** 商品收藏变更后触发，用于同步收藏列表页等 */
export const GOODS_COLLECT_CHANGED_EVENT = 'goods-collect-changed'

export function dispatchGoodsCollectChanged() {
  window.dispatchEvent(new CustomEvent(GOODS_COLLECT_CHANGED_EVENT))
}

/** 店铺收藏变更后触发，用于同步收藏列表页等 */
export const SHOP_COLLECT_CHANGED_EVENT = 'shop-collect-changed'

export function dispatchShopCollectChanged() {
  window.dispatchEvent(new CustomEvent(SHOP_COLLECT_CHANGED_EVENT))
}
