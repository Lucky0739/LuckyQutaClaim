package com.lucky.seckill.order.service;

import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.order.model.dto.AddOrderRequest;
import com.lucky.seckill.order.model.dto.OrderQueryRequest;
import com.lucky.seckill.order.model.entity.Order;
import com.lucky.seckill.order.model.vo.OrderVO;

import java.util.List;

public interface OrderService {

    /**
     * 创建订单（下单）
     * @param addOrderRequest 订单请求
     * @return 订单ID
     */
    Response<Long> addOrder(AddOrderRequest addOrderRequest);

    /**
     * 取消订单（释放预占的库存）
     * @param orderNo 订单号
     * @return true=处理成功
     */
    Response<Boolean> cancelOrder(String orderNo);

    /**
     * 根据订单号查询订单
     * @param orderNo 订单号
     * @return 订单信息
     */
    Response<Order> getOrderByOrderNo(String orderNo);

    /**
     * 根据订单号查询订单（VO格式）
     * @param orderNo 订单号
     * @return 订单VO
     */
    Response<OrderVO> getOrderVOByOrderNo(String orderNo);

    /**
     * 更新订单状态
     * @param orderNo 订单号
     * @param status 状态
     * @return true=更新成功
     */
    Response<Boolean> updateOrderStatus(String orderNo, Integer status);

    /**
     * 商家发货
     * @param orderNo 订单号
     * @return true=发货成功
     */
    Response<Boolean> deliverOrder(String orderNo);

    /**
     * 用户确认收货
     * @param orderNo 订单号
     * @return true=确认成功
     */
    Response<Boolean> confirmReceipt(String orderNo);

    /**
     * 查询当前用户的订单列表
     * @param request 查询条件
     * @return 订单列表
     */
    Response<PageResult<OrderVO>> getOrderList(OrderQueryRequest request);

    /**
     * 商家查询自己商品的订单列表
     * @param request 查询条件
     * @return 订单列表
     */
    Response<PageResult<OrderVO>> getMerchantOrderList(OrderQueryRequest request);

    /**
     * 根据订单ID查询订单
     * @param orderId 订单ID
     * @return 订单信息
     */
    Response<Order> getOrderById(Long orderId);

    /**
     * 批量查询订单
     * @param orderIds 订单ID列表
     * @return 订单列表
     */
    Response<List<Order>> getOrdersByIds(List<Long> orderIds);

    /**
     * 根据商品ID查询店铺ID
     * @param goodsId 商品ID
     * @return 店铺ID
     */
    Response<Long> getShopIdByGoodsId(Long goodsId);

    /**
     * 关闭过期订单
     * @param orderNo 订单号
     * @return true=处理成功
     */
    Response<Boolean> closeExpiredOrder(String orderNo);

    /**
     * 批量关闭超时未支付订单（定时任务调用）
     */
    void closeExpiredOrders();
}
