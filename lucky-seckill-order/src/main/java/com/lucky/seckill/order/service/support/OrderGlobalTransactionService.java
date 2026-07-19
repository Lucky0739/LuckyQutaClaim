package com.lucky.seckill.order.service.support;

import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.feign.GoodsFeignClient;
import com.lucky.seckill.order.enums.OrderStatusEnum;
import com.lucky.seckill.order.mapper.OrderMapper;
import com.lucky.seckill.order.model.entity.Order;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

/**
 * 订单跨服务分布式事务（Seata AT）：订单库 + 商品服务库存 Feign。
 * 独立 Bean，避免在 OrderServiceImpl 自调用导致 @GlobalTransactional 失效。
 */
@Service
@Slf4j
public class OrderGlobalTransactionService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private GoodsFeignClient goodsFeignClient;

    @GlobalTransactional(name = "order-create", rollbackFor = Exception.class)
    public void lockStockAndInsertOrder(Order order, Long skuId, Integer goodsNum) {
        log.info("[Seata] 下单全局事务, orderNo={}, skuId={}, num={}", order.getOrderNo(), skuId, goodsNum);
        Response<Boolean> lockResult = goodsFeignClient.lockStock(skuId, goodsNum);
        if (lockResult == null || !lockResult.isSuccess() || !Boolean.TRUE.equals(lockResult.getData())) {
            throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "库存不足，下单失败");
        }
        orderMapper.insertSelective(order);
    }

    @GlobalTransactional(name = "order-cancel", rollbackFor = Exception.class)
    public void updateStatusAndReleaseStock(String orderNo, Integer status, Long skuId, Integer buyGoodsNum) {
        log.info("[Seata] 取消/关闭订单全局事务, orderNo={}, status={}", orderNo, status);
        int rows = orderMapper.updateStatusByOrderNo(orderNo, status);
        if (rows <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "更新订单状态失败");
        }
        releaseStockOrThrow(orderNo, skuId, buyGoodsNum);
    }

    @GlobalTransactional(name = "order-pay-success", rollbackFor = Exception.class)
    public void deductStockAndMarkPaid(String orderNo, Long skuId, Integer buyGoodsNum) {
        log.info("[Seata] 支付成功全局事务, orderNo={}, skuId={}, num={}", orderNo, skuId, buyGoodsNum);
        if (skuId != null && buyGoodsNum != null && buyGoodsNum > 0) {
            Response<Boolean> deductResult = goodsFeignClient.deductStock(skuId, buyGoodsNum);
            if (deductResult == null || !deductResult.isSuccess() || !Boolean.TRUE.equals(deductResult.getData())) {
                throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "扣减库存失败");
            }
        }
        int rows = orderMapper.updateStatusByOrderNo(orderNo, OrderStatusEnum.PAID.getCode());
        if (rows <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "更新订单状态失败");
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
