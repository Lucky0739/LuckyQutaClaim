package com.lucky.seckill.pay.service.impl;

import com.alipay.api.response.AlipayTradeQueryResponse;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.order.model.entity.Order;
import com.lucky.seckill.pay.feign.GoodsFeignClient;
import com.lucky.seckill.pay.feign.OrderFeignClient;
import com.lucky.seckill.pay.service.PayOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.Factory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PayOrderServiceImpl implements PayOrderService {

    @Resource
    private OrderFeignClient orderFeignClient;

    @Resource
    private GoodsFeignClient goodsFeignClient;

    @Value("${alipay.notify-url:}")
    private String notifyUrl;

    public static final int ORDER_STATUS_PAID = 1;

    @Override
    public Response<Boolean> payOrder(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }

        log.info("==> 查询支付状态，orderNo: {}", orderNo);

        try {
            AlipayTradeQueryResponse response = Factory.Payment.Common()
                    .query(orderNo);

            if (ResponseChecker.success(response)) {
                String tradeStatus = response.tradeStatus;
                log.info("==> 查询支付状态成功，orderNo: {}, tradeStatus: {}", orderNo, tradeStatus);

                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    return payCallback(orderNo);
                }
                return Response.success(false);
            } else {
                log.warn("==> 查询支付状态失败，orderNo: {}", orderNo);
                return Response.success(false);
            }
        } catch (Exception e) {
            log.error("==> 查询支付状态异常，orderNo: {}", orderNo, e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询支付状态异常");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> payCallback(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }

        log.info("==> 支付成功回调处理，orderNo: {}", orderNo);

        Response<Order> orderResponse = orderFeignClient.getOrderByOrderNo(orderNo);
        if (orderResponse == null || !orderResponse.isSuccess() || orderResponse.getData() == null) {
            log.error("==> 查询订单失败，orderNo: {}", orderNo);
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单不存在");
        }

        Order order = orderResponse.getData();

        Response<Boolean> deductResult = goodsFeignClient.deductStock(order.getSkuId(), order.getLockStockNum());
        if (deductResult == null || !deductResult.isSuccess() || deductResult.getData() == null || !deductResult.getData()) {
            log.error("==> 扣减库存失败，orderNo: {}, skuId: {}, goodsNum: {}",
                    orderNo, order.getSkuId(), order.getLockStockNum());
            throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "扣减库存失败");
        }

        Response<Boolean> statusResult = orderFeignClient.updateOrderStatus(orderNo, ORDER_STATUS_PAID);
        if (statusResult == null || !statusResult.isSuccess() || !statusResult.getData()) {
            log.error("==> 更新订单状态失败，orderNo: {}", orderNo);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "更新订单状态失败");
        }

        log.info("==> 支付回调处理成功，orderNo: {}, skuId: {}, goodsNum: {}",
                orderNo, order.getSkuId(), order.getLockStockNum());
        return Response.success(true);
    }
}
