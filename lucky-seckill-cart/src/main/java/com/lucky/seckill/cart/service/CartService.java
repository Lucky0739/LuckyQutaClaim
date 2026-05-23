package com.lucky.seckill.cart.service;

import com.lucky.seckill.cart.model.dto.AddCartRequest;
import com.lucky.seckill.cart.model.vo.CartVO;
import com.lucky.seckill.common.utils.Response;

import java.util.List;

public interface CartService {
    /**
     * 添加商品到购物车
     * @param request
     * @return
     */
    Response<Void> addCart(AddCartRequest request);

    /**
     * 查询用户购物车列表
     * @return
     */
    Response<List<CartVO>> getCartList();

    /**
     * 删除购物车商品
     * @param cartId
     * @return
     */
    Response<Void> deleteCart(Long cartId);

    /**
     * 更新购物车商品数量
     * @param cartId
     * @param goodsCount
     * @return
     */
    Response<Void> updateQuantity(Long cartId, Integer goodsCount);

    /**
     * 选中/取消选中购物车商品
     * @param cartId
     * @param selected
     * @return
     */
    Response<Void> selectCart(Long cartId, Integer selected);

    /**
     * 全选/取消全选购物车
     * @param selected
     * @return
     */
    Response<Void> selectAll(Integer selected);

    /**
     * 批量删除购物车商品
     * @param cartIds
     * @return
     */
    Response<Void> batchDeleteCart(List<Long> cartIds);
}
