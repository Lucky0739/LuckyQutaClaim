package com.lucky.seckill.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.feign.GoodsFeignClient;
import com.lucky.seckill.goods.model.entity.Goods;
import com.lucky.seckill.goods.model.entity.GoodsSku;
import com.lucky.seckill.user.feign.UserAddressFeignClient;
import com.lucky.seckill.user.model.entity.UserAddress;
import com.lucky.seckill.order.enums.OrderStatusEnum;
import com.lucky.seckill.order.mapper.OrderMapper;
import com.lucky.seckill.order.model.dto.AddOrderRequest;
import com.lucky.seckill.order.model.dto.OrderQueryRequest;
import com.lucky.seckill.order.model.entity.Order;
import com.lucky.seckill.order.model.vo.OrderVO;
import com.lucky.seckill.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private GoodsFeignClient goodsFeignClient;
    @Resource
    private UserAddressFeignClient userAddressFeignClient;
    @Resource
    private ObjectMapper objectMapper;
    /**
     * 创建订单（下单）
     */
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
        if (addOrderRequest.getUserAddressId() == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "收货地址ID不能为空");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        Long skuId = addOrderRequest.getSkuId();
        Integer goodsNum = addOrderRequest.getBuyGoodsNum();

        // 查询商品信息
        Goods goods = null;
        try {
            Object goodsObj = goodsFeignClient.getGoodsById(addOrderRequest.getGoodsId());
            if (goodsObj instanceof Response) {
                Response<?> response = (Response<?>) goodsObj;
                if (response.isSuccess() && response.getData() != null) {
                    goods = objectMapper.convertValue(response.getData(), Goods.class);
                }
            }
        } catch (Exception e) {
            log.error("==> 查询商品信息失败，goodsId: {}", addOrderRequest.getGoodsId(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询商品信息失败");
        }
        if (goods == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品不存在");
        }

        // 查询SKU信息
        GoodsSku goodsSku = null;
        try {
            Object skuObj = goodsFeignClient.getSkuById(skuId);
            if (skuObj instanceof Response) {
                Response<?> response = (Response<?>) skuObj;
                if (response.isSuccess() && response.getData() != null) {
                    goodsSku = objectMapper.convertValue(response.getData(), GoodsSku.class);
                }
            }
        } catch (Exception e) {
            log.error("==> 查询SKU信息失败，skuId: {}", skuId, e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询SKU信息失败");
        }
        if (goodsSku == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品规格不存在");
        }

        // 查询收货地址
        UserAddress userAddress = null;
        try {
            Response<UserAddress> addressResponse = userAddressFeignClient.getAddressById(addOrderRequest.getUserAddressId());
            if (addressResponse != null && addressResponse.isSuccess() && addressResponse.getData() != null) {
                userAddress = addressResponse.getData();
            }
        } catch (Exception e) {
            log.error("==> 查询收货地址失败，userAddressId: {}", addOrderRequest.getUserAddressId(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询收货地址失败");
        }
        if (userAddress == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "收货地址不存在或无权使用");
        }

        // 预占库存
        Response<Boolean> lockResult = goodsFeignClient.lockStock(skuId, goodsNum);
        if (lockResult == null || !lockResult.isSuccess() || lockResult.getData() == null || !lockResult.getData()) {
            log.warn("==> 预占库存失败，库存不足");
            throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "库存不足，下单失败");
        }

        // 创建订单
        long orderId = IdUtil.getSnowflakeNextId();
        String orderNo = generateOrderNo(userId);
        BigDecimal totalAmount = goodsSku.getSalePrice().multiply(BigDecimal.valueOf(goodsNum));
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);

        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .goodsId(addOrderRequest.getGoodsId())
                .skuId(skuId)
                .userAddressId(addOrderRequest.getUserAddressId())
                .orderNo(orderNo)
                .goodsPrice(goodsSku.getSalePrice())
                .buyGoodsNum(goodsNum)
                .totalAmount(totalAmount)
                .goodsName(goods.getGoodsName())
                .goodsImg(goods.getGoodsImg())
                .status(OrderStatusEnum.PENDING_PAYMENT.getCode())
                .mobile(userAddress.getMobile())
                .receiver(userAddress.getReceiver())
                .province(userAddress.getProvince())
                .city(userAddress.getCity())
                .area(userAddress.getArea())
                .detailAddress(userAddress.getDetailAddress())
                .expireTime(expireTime)
                .isDelete(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        orderMapper.insertSelective(order);

        log.info("==> 创建订单成功，orderId: {}, orderNo: {}", orderId, orderNo);
        return Response.success(orderId);
    }

    /**
     * 取消订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> cancelOrder(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单不存在");
        }

        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            log.warn("==> 订单状态不是待支付，无法取消，orderNo: {}", orderNo);
            throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "订单状态异常，无法取消");
        }

        orderMapper.updateStatusByOrderNo(orderNo, OrderStatusEnum.CANCELLED.getCode());

        log.info("==> 订单已取消，orderNo: {}", orderNo);
        return Response.success(true);
    }

    /**
     * 查询订单信息
     * @param orderNo 订单号
     * @return
     */
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
    public Response<OrderVO> getOrderVOByOrderNo(String orderNo) {
        Order order = getOrderByOrderNo(orderNo).getData();
        return Response.success(OrderVO.fromEntity(order));
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

        if (OrderStatusEnum.PAID.getCode().equals(status)) {
            if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
                throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "订单状态异常，无法完成支付");
            }
            if (order.getSkuId() != null && order.getBuyGoodsNum() != null && order.getBuyGoodsNum() > 0) {
                Response<Boolean> deductResult = goodsFeignClient.deductStock(order.getSkuId(), order.getBuyGoodsNum());
                if (deductResult == null || !deductResult.isSuccess()
                        || deductResult.getData() == null || !deductResult.getData()) {
                    log.error("==> 支付扣减库存失败，orderNo: {}, skuId: {}", orderNo, order.getSkuId());
                    throw new BizException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "扣减库存失败");
                }
            }
        }

        int result = orderMapper.updateStatusByOrderNo(orderNo, status);
        if (result > 0) {
            log.info("==> 更新订单状态成功，orderNo: {}, status: {}", orderNo, status);
            return Response.success(true);
        }

        log.warn("==> 更新订单状态失败，orderNo: {}, status: {}", orderNo, status);
        return Response.success(false);
    }

    @Override
    public Response<PageResult<OrderVO>> getOrderList(OrderQueryRequest request) {
        if (request == null) {
            request = new OrderQueryRequest();
        }
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }

        Long userId = StpUtil.getLoginIdAsLong();

        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<Order> orderList = orderMapper.selectOrdersByUserId(userId, request.getStatus(), request.getGoodsName());
        PageInfo<Order> pageInfo = new PageInfo<>(orderList);

        List<OrderVO> voList = orderList.stream()
                .map(OrderVO::fromEntity)
                .collect(Collectors.toList());

        PageResult<OrderVO> result = PageResult.<OrderVO>builder()
                .records(voList)
                .pageNum(pageInfo.getPageNum())
                .pageSize(pageInfo.getPageSize())
                .total(pageInfo.getTotal())
                .totalPages(pageInfo.getPages())
                .build();

        return Response.success(result);
    }

    @Override
    public Response<Order> getOrderById(Long orderId) {
        if (orderId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单ID不能为空");
        }

        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单不存在");
        }

        return Response.success(order);
    }

    @Override
    public Response<List<Order>> getOrdersByIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单ID列表不能为空");
        }

        List<Order> orders = orderMapper.selectByIds(orderIds);
        return Response.success(orders);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> closeExpiredOrder(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单不存在");
        }

        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            log.warn("==> 订单状态不是待支付，无需关闭，orderNo: {}", orderNo);
            return Response.success(true);
        }

        if (order.getExpireTime() == null || LocalDateTime.now().isBefore(order.getExpireTime())) {
            log.warn("==> 订单尚未过期，orderNo: {}", orderNo);
            return Response.success(true);
        }

        orderMapper.updateStatusByOrderNo(orderNo, OrderStatusEnum.EXPIRED.getCode());

        log.info("==> 过期订单已关闭，orderNo: {}", orderNo);
        return Response.success(true);
    }

    private String generateOrderNo(Long userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String userIdStr = String.format("%04d", userId % 10000);
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD" + timestamp + userIdStr + random;
    }
}
