package com.lucky.seckill.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.feign.GoodsFeignClient;
import com.lucky.seckill.goods.model.entity.Goods;
import com.lucky.seckill.goods.model.entity.GoodsSku;
import com.lucky.seckill.order.mapper.OrderMapper;
import com.lucky.seckill.order.model.dto.AddOrderRequest;
import com.lucky.seckill.order.model.entity.Order;
import com.lucky.seckill.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private GoodsFeignClient goodsFeignClient;

    /**
     * 订单状态枚举
     */
    public static final int ORDER_STATUS_PENDING_PAYMENT = 0;   // 待支付
    public static final int ORDER_STATUS_PAID = 1;              // 已支付
    public static final int ORDER_STATUS_CANCELLED = 2;         // 已取消
    public static final int ORDER_STATUS_EXPIRED = 3;           // 已过期

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> addOrder(AddOrderRequest addOrderRequest) {
        if (addOrderRequest.getGoodsId() == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品ID不能为空");
        }
        if (addOrderRequest.getSkuId() == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU ID不能为空");
        }
        if (addOrderRequest.getBuyGoodsNum() == null || addOrderRequest.getBuyGoodsNum() <= 0) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品数量必须大于0");
        }

        // 获取登录用户id
        Long userId = StpUtil.getLoginIdAsLong();
        // 商品id
        Long goodsId = addOrderRequest.getGoodsId();
        // 获取商品规格id
        Long skuId = addOrderRequest.getSkuId();
        // 购买商品数量
        Integer buyGoodsNum = addOrderRequest.getBuyGoodsNum();

        // 查询商品信息
        Goods goods = null;
        try {
            log.info("==> 开始调用 goodsFeignClient.getGoodsById，goodsId: {}", goodsId);
            if (goodsFeignClient == null) {
                log.error("==> goodsFeignClient 为 null！");
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "商品服务客户端未初始化");
            }
            
            Response<Goods> response = goodsFeignClient.getGoodsById(goodsId);
            if (response == null) {
                log.error("==> Feign调用返回为 null");
            } else {
                boolean success = response.isSuccess();
                Object data = response.getData();
                String message = response.getMessage();
                String errorCode = response.getErrorCode();
                
                log.info("==> response.isSuccess() = {}", success);
                log.info("==> response.getMessage() = {}", message);
                log.info("==> response.getErrorCode() = {}", errorCode);
                log.info("==> response.getData() = {}", data);
                
                if (data != null) {
                    Goods dataGoods = response.getData();
                    log.info("==> 数据类型: {}", data.getClass().getName());
                    log.info("==> 商品ID: {}", dataGoods.getId());
                    log.info("==> 商品名称: {}", dataGoods.getGoodsName());
                }
                
                if (success && data != null) {
                    goods = response.getData();
                    log.info("==> 成功获取商品信息: id={}, goodsName={}", goods.getId(), goods.getGoodsName());
                } else {
                    log.warn("==> 未获取商品信息，success={}, data={}", success, data);
                }
            }
            log.info("==> 最终 goods 值：{}", goods);
        } catch (Exception e) {
            log.error("==> 查询商品信息失败，goodsId: {}, 异常类型: {}, 异常消息: {}", 
                    goodsId, e.getClass().getName(), e.getMessage(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询商品信息失败");
        }
        if (goods == null) {
            log.error("==> goods 为 null，抛出商品不存在异常");
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品不存在");
        }

        // 查询SKU信息
        GoodsSku goodsSku = null;
        try {
            Response<GoodsSku> response = goodsFeignClient.getSkuById(skuId);
            if (response != null && response.isSuccess() && response.getData() != null) {
                goodsSku = response.getData();
            }
        } catch (Exception e) {
            log.error("==> 查询SKU信息失败，skuId: {}", skuId, e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询SKU信息失败");
        }
        if (goodsSku == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品规格不存在");
        }

        // 1. 预占库存（使用行锁防止超卖）
        Response<Boolean> lockResult = goodsFeignClient.lockStock(skuId,buyGoodsNum);
        if (lockResult == null || !lockResult.isSuccess() || lockResult.getData() == null || !lockResult.getData()) {
            log.warn("==> 预占库存失败，库存不足，skuId: {}, goodsNum: {}", skuId, buyGoodsNum);
            throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "库存不足，下单失败");
        }
        // 2. 订单id经过雪花算法处理
        long orderId = IdUtil.getSnowflakeNextId();
        // 订单号
        String orderNo = generateOrderNo(userId);
        // 商品总价（使用SKU的售价）
        BigDecimal totalAmount = goodsSku.getSalePrice().multiply(BigDecimal.valueOf(buyGoodsNum));
        // 订单过期时间
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);

        // 3. 创建订单（状态为待支付）
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .goodsId(addOrderRequest.getGoodsId())
                .skuId(skuId)
                .orderNo(orderNo)
                .goodsPrice(goodsSku.getSalePrice())
                .buyGoodsNum(buyGoodsNum)
                .totalAmount(totalAmount)
                .goodsName(goods.getGoodsName())
                .goodsImg(goods.getGoodsImg())
                .status(ORDER_STATUS_PENDING_PAYMENT)
                .expireTime(expireTime)
                .isDelete(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        orderMapper.insertSelective(order);

        log.info("==> 创建订单成功（已预占库存），orderId: {}, orderNo: {}, skuId: {}, goodsNum: {}",
                orderId, orderNo, skuId, buyGoodsNum);
        return Response.success(orderId);
    }

    /**
     * 支付成功回调
     * @param orderNo 订单号
     * @return true=处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> paySuccess(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }

        // 1. 查询订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单不存在");
        }

        // 2. 校验订单状态
        if (order.getStatus() != ORDER_STATUS_PENDING_PAYMENT) {
            log.warn("==> 订单状态不是待支付，无法处理支付回调，orderNo: {}, status: {}", orderNo, order.getStatus());
            throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "订单状态异常");
        }

        // 3. 检查订单是否已过期
        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            log.warn("==> 订单已过期，orderNo: {}, expireTime: {}", orderNo, order.getExpireTime());
            // 释放预占的库存
            goodsFeignClient.releaseStock(order.getSkuId(), order.getBuyGoodsNum());
            // 更新订单状态为已过期
            orderMapper.updateStatusByOrderNo(orderNo, ORDER_STATUS_EXPIRED);
            throw new BizException(ResponseCodeEnum.ORDER_EXPIRED, "订单已过期");
        }

        // 4. 真正扣减库存（此时预占的库存被正式扣减，salesNum增加）
        Response<Boolean> deductResult = goodsFeignClient.deductStock(order.getSkuId(), order.getBuyGoodsNum());
        if (deductResult == null || !deductResult.isSuccess() || deductResult.getData() == null || !deductResult.getData()) {
            log.error("==> 扣减库存失败，orderNo: {}, skuId: {}, goodsNum: {}",
                    orderNo, order.getSkuId(), order.getBuyGoodsNum());
            throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "扣减库存失败");
        }

        // 5. 更新订单状态为已支付
        Order updateOrder = Order.builder()
                .id(order.getId())
                .orderNo(orderNo)
                .status(ORDER_STATUS_PAID)
                .payTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        orderMapper.updateByPrimaryKeySelective(updateOrder);

        log.info("==> 支付成功，订单已完成，orderNo: {}, skuId: {}, goodsNum: {}",
                orderNo, order.getSkuId(), order.getBuyGoodsNum());
        return Response.success(true);
    }

    /**
     * 取消订单（释放预占的库存）
     * @param orderNo 订单号
     * @return true=处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> cancelOrder(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }

        // 1. 查询订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单不存在");
        }

        // 2. 校验订单状态（只能取消待支付的订单）
        if (order.getStatus() != ORDER_STATUS_PENDING_PAYMENT) {
            log.warn("==> 订单状态不是待支付，无法取消，orderNo: {}, status: {}", orderNo, order.getStatus());
            throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "订单状态异常，无法取消");
        }

        // 3. 释放预占的库存
        if (order.getSkuId() != null && order.getBuyGoodsNum() != null && order.getBuyGoodsNum() > 0) {
            Response<Boolean> releaseResult = goodsFeignClient.releaseStock(order.getSkuId(), order.getBuyGoodsNum());
            if (releaseResult == null || !releaseResult.isSuccess()) {
                log.error("==> 释放库存失败，orderNo: {}, skuId: {}, goodsNum: {}",
                        orderNo, order.getSkuId(), order.getBuyGoodsNum());
            }
        }

        // 4. 更新订单状态为已取消
        orderMapper.updateStatusByOrderNo(orderNo, ORDER_STATUS_CANCELLED);

        log.info("==> 订单已取消，orderNo: {}", orderNo);
        return Response.success(true);
    }

    @Override
    public Response<Order> getOrderByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单不存在");
        }

        return Response.success(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> updateOrderStatus(String orderNo, Integer status) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }
        if (status == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单状态不能为空");
        }

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单不存在");
        }

        int result = orderMapper.updateStatusByOrderNo(orderNo, status);
        if (result > 0) {
            log.info("==> 更新订单状态成功，orderNo: {}, status: {}", orderNo, status);
            return Response.success(true);
        }

        log.warn("==> 更新订单状态失败，orderNo: {}, status: {}", orderNo, status);
        return Response.success(false);
    }

    private String generateOrderNo(Long userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String userIdStr = String.format("%04d", userId % 10000);
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD" + timestamp + userIdStr + random;
    }
}
