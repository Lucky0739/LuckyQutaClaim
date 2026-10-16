/** 购物车变更后触发，用于同步头部角标等 */
export const CART_CHANGED_EVENT = 'cart-changed'

export function dispatchCartChanged() {
  window.dispatchEvent(new CustomEvent(CART_CHANGED_EVENT))
}
