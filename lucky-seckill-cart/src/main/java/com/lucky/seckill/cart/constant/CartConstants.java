package com.lucky.seckill.cart.constant;

/**
 * 购物车常量
 */
public final class CartConstants {

    private CartConstants() {
    }

    /** 单条购物车商品最大数量 */
    public static final int MAX_GOODS_COUNT_PER_ITEM = 999;

    /** 商品上架状态 */
    public static final int GOODS_STATUS_ON_SALE = 1;

    /** 商品审核通过 */
    public static final int GOODS_AUDIT_PASS = 2;

    /** SKU 启用状态 */
    public static final int SKU_STATUS_ENABLED = 1;
}
