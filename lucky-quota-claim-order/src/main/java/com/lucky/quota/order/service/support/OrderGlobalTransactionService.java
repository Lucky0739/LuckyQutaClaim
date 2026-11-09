package com.lucky.quota.order.service.support;

import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.feign.GoodsFeignClient;
import com.lucky.quota.order.constant.OrderConstants;
import com.lucky.quota.order.enums.OrderStatusEnum;
import com.lucky.quota.order.mapper.OrderMapper;
import com.lucky.quota.order.model.entity.Order;
import com.lucky.quota.order.payment.exception.OrderNotPayableException;
import com.lucky.quota.user.feign.UserBalanceFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申领单跨服务分布式事务（Seata AT）。
 * 独立 Bean，避免在 OrderServiceImpl 自调用导致 @GlobalTransactional 失效。
 * 余额支付同时覆盖用户余额、申领单状态和物资库存，任一步失败由 Seata 回滚。
 */
@Service
@Slf4j
public class OrderGlobalTransactionService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private GoodsFeignClient goodsFeignClient;
    @Resource
    private UserBalanceFeignClient userBalanceFeignClient;

    /**
     * 余额支付：扣用户余额、申领单改为已支付、扣减物资库存。
     */
    @GlobalTransactional(name = "order-balance-pay", rollbackFor = Exception.class)
    public void deductBalanceAndSettle(Long userId, BigDecimal payAmount, String orderNo,
                                       LocalDateTime payTime, Long skuId, Integer buyGoodsNum) {
        log.info("[Seata] 余额支付全局事务, orderNo={}, userId={}, amount={}", orderNo, userId, payAmount);
        Response<Boolean> deductResp = userBalanceFeignClient.deductBalance(userId, payAmount);
        if (deductResp == null || !deductResp.isSuccess() || !Boolean.TRUE.equals(deductResp.getData())) {
            String msg = deductResp != null && StringUtils.hasText(deductResp.getMessage())
                    ? deductResp.getMessage() : "账户余额不足";
            throw new BizException(ResponseCodeEnum.BALANCE_NOT_ENOUGH, msg);
        }
        deductStockAndMarkPaidAfterPayment(orderNo, payTime, skuId, buyGoodsNum);
    }

    @GlobalTransactional(name = "order-create", rollbackFor = Exception.class)
    public void lockStockAndInsertOrder(Order order, Long skuId, Integer goodsNum) {
        log.info("[Seata] 申领全局事务, orderNo={}, skuId={}, num={}", order.getOrderNo(), skuId, goodsNum);
        Response<Boolean> lockResult = goodsFeignClient.lockStock(skuId, goodsNum);
        if (lockResult == null || !lockResult.isSuccess() || !Boolean.TRUE.equals(lockResult.getData())) {
            throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "库存不足，申领失败");
        }
        orderMapper.insertSelective(order);
    }

    @GlobalTransactional(name = "order-cancel", rollbackFor = Exception.class)
    public void updateStatusAndReleaseStock(String orderNo, Integer expectedStatus, Integer targetStatus,
                                            Long skuId, Integer buyGoodsNum) {
        log.info("[Seata] 取消/关闭申领单全局事务, orderNo={}, expectedStatus={}, targetStatus={}",
                orderNo, expectedStatus, targetStatus);
        // 条件更新：只有仍处于预期状态才允许取消/关闭，避免覆盖并发的支付成功
        int rows = orderMapper.updateStatusIfCurrent(orderNo, expectedStatus, targetStatus);
        if (rows <= 0) {
            throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "申领单状态已变更");
        }
        if (OrderConstants.isQuotaOrderNo(orderNo)) {
            log.info("==> 限量单取消/关闭，跳过释放预占库存, orderNo={}", orderNo);
            return;
        }
        releaseStockOrThrow(orderNo, skuId, buyGoodsNum);
    }

    @GlobalTransactional(name = "order-pay-success", rollbackFor = Exception.class)
    public void deductStockAndMarkPaid(String orderNo, Long skuId, Integer buyGoodsNum) {
        log.info("[Seata] 确认领取全局事务, orderNo={}, skuId={}, num={}", orderNo, skuId, buyGoodsNum);
        if (!OrderConstants.isQuotaOrderNo(orderNo)
                && skuId != null && buyGoodsNum != null && buyGoodsNum > 0) {
            Response<Boolean> deductResult = goodsFeignClient.deductStock(skuId, buyGoodsNum);
            if (deductResult == null || !deductResult.isSuccess() || !Boolean.TRUE.equals(deductResult.getData())) {
                throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "扣减库存失败");
            }
        }
        int rows = orderMapper.updateStatusByOrderNo(orderNo, OrderStatusEnum.PAID.getCode());
        if (rows <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "更新申领状态失败");
        }
    }

    /**
     * 支付成功入账：先条件更新订单（0 -> 1 并写 pay_time），成功后再扣减库存。
     * 条件更新作为闸门，保证过期/取消的订单不会被支付"复活"；扣库存失败时 Seata 会回滚订单更新。
     */
    @GlobalTransactional(name = "order-payment-success", rollbackFor = Exception.class)
    public void deductStockAndMarkPaidAfterPayment(String orderNo, LocalDateTime payTime,
                                                   Long skuId, Integer buyGoodsNum) {
        log.info("[Seata] 支付成功入账全局事务, orderNo={}, skuId={}, num={}", orderNo, skuId, buyGoodsNum);
        int rows = orderMapper.markPaidIfPending(orderNo, payTime);
        if (rows <= 0) {
            throw new OrderNotPayableException(orderNo);
        }
        if (!OrderConstants.isQuotaOrderNo(orderNo)
                && skuId != null && buyGoodsNum != null && buyGoodsNum > 0) {
            Response<Boolean> deductResult = goodsFeignClient.deductStock(skuId, buyGoodsNum);
            if (deductResult == null || !deductResult.isSuccess() || !Boolean.TRUE.equals(deductResult.getData())) {
                throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "扣减库存失败");
            }
        }
    }

    private void releaseStockOrThrow(String orderNo, Long skuId, Integer buyGoodsNum) {
        if (skuId == null || buyGoodsNum == null || buyGoodsNum <= 0) {
            return;
        }
        Response<Boolean> releaseResult = goodsFeignClient.releaseStock(skuId, buyGoodsNum);
        if (releaseResult == null || !releaseResult.isSuccess() || !Boolean.TRUE.equals(releaseResult.getData())) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "释放预占库存失败");
        }
        log.info("==> 释放预占库存成功，orderNo: {}, skuId: {}, num: {}", orderNo, skuId, buyGoodsNum);
    }
}
