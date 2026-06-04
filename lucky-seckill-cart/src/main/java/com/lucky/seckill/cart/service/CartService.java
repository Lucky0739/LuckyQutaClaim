package com.lucky.seckill.cart.service;

import com.lucky.seckill.cart.model.dto.AddCartRequest;
import com.lucky.seckill.cart.model.dto.BatchDeleteCartRequest;
import com.lucky.seckill.cart.model.vo.CartListVO;
import com.lucky.seckill.common.utils.Response;

import java.util.List;

public interface CartService {

    Response<Void> addCart(AddCartRequest request);

    Response<CartListVO> getCartList();

    Response<Integer> getCartCount();

    Response<Void> deleteCart(Long cartId);

    Response<Void> updateQuantity(Long cartId, Integer goodsCount);

    Response<Void> selectCart(Long cartId, Integer selected);

    Response<Void> selectAll(Integer selected);

    Response<Void> batchDeleteCart(List<Long> cartIds);

    Response<Void> batchDeleteCart(BatchDeleteCartRequest request);

    Response<Void> deleteSelected();

    Response<Void> clearCart();
}
