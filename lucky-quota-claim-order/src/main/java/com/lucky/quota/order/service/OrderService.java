package com.lucky.quota.order.service;

import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.order.model.dto.AddOrderRequest;
import com.lucky.quota.order.model.dto.OrderQueryRequest;
import com.lucky.quota.order.model.entity.Order;
import com.lucky.quota.order.model.vo.OrderVO;

import java.util.List;

public interface OrderService {

    /**
     * 创建申领单
     * @param addOrderRequest 申领单请求
     * @return 申领单ID
     */
    Response<Long> addOrder(AddOrderRequest addOrderRequest);

    /**
     * 取消申领（释放预占库存）
     */
    Response<Boolean> cancelOrder(String orderNo);

    /**
     * 确认领取：待确认 → 待发放，并正式扣库存。超时未确认由 RocketMQ 延迟消息关单。
     */
    Response<Boolean> confirmClaim(String orderNo);

    /**
     * 根据申领单号查询申领单
     * @param orderNo 申领单号
     * @return 申领单信息
     */
    Response<Order> getOrderByOrderNo(String orderNo);

    /**
     * 根据申领单号查询申领单（VO格式）
     * @param orderNo 申领单号
     * @return 申领单VO
     */
    Response<OrderVO> getOrderVOByOrderNo(String orderNo);

    /**
     * 更新申领状态
     * @param orderNo 申领单号
     * @param status 状态
     * @return true=更新成功
     */
    Response<Boolean> updateOrderStatus(String orderNo, Integer status);

    /**
     * 商家发放
     * @param orderNo 申领单号
     * @return true=发放成功
     */
    Response<Boolean> deliverOrder(String orderNo);

    /**
     * 用户确认到货
     * @param orderNo 申领单号
     * @return true=确认成功
     */
    Response<Boolean> confirmReceipt(String orderNo);

    /**
     * 查询当前用户的申领列表
     * @param request 查询条件
     * @return 申领列表
     */
    Response<PageResult<OrderVO>> getOrderList(OrderQueryRequest request);

    /**
     * 商家查询自己物资的申领列表
     * @param request 查询条件
     * @return 申领列表
     */
    Response<PageResult<OrderVO>> getMerchantOrderList(OrderQueryRequest request);

    /**
     * 根据申领单ID查询申领单
     * @param orderId 申领单ID
     * @return 申领单信息
     */
    Response<Order> getOrderById(Long orderId);

    /**
     * 批量查询申领单
     * @param orderIds 申领单ID列表
     * @return 申领列表
     */
    Response<List<Order>> getOrdersByIds(List<Long> orderIds);

    /**
     * 根据物资ID查询发放单位ID
     * @param goodsId 物资ID
     * @return 发放单位ID
     */
    Response<Long> getShopIdByGoodsId(Long goodsId);

    /**
     * 关闭过期申领单
     * @param orderNo 申领单号
     * @return true=处理成功
     */
    Response<Boolean> closeExpiredOrder(String orderNo);
}
