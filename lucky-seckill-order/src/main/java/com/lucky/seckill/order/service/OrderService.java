package com.lucky.seckill.order.service;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.order.model.dto.AddOrderRequest;
import com.lucky.seckill.order.model.entity.Order;

public interface OrderService {

    Response<Long> addOrder(AddOrderRequest addOrderRequest);

    Response<Boolean> paySuccess(String orderNo);

    Response<Boolean> cancelOrder(String orderNo);

    Response<Order> getOrderByOrderNo(String orderNo);

    Response<Boolean> updateOrderStatus(String orderNo, Integer status);
}