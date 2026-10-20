package com.lucky.quota.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.common.model.entity.OrderDeliveryMessage;
import com.lucky.quota.common.model.entity.OrderReceiveMessage;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.feign.GoodsFeignClient;
import com.lucky.quota.goods.model.entity.Goods;
import com.lucky.quota.goods.model.entity.GoodsSku;
import com.lucky.quota.user.feign.UserAddressFeignClient;
import com.lucky.quota.user.model.entity.UserAddress;
import com.lucky.quota.order.feign.ActivityFeignClient;
import com.lucky.quota.order.constant.OrderConstants;
import com.lucky.quota.order.enums.OrderStatusEnum;
import com.lucky.quota.order.mapper.OrderMapper;
import com.lucky.quota.order.model.dto.AddOrderRequest;
import com.lucky.quota.order.model.dto.OrderQueryRequest;
import com.lucky.quota.order.model.entity.Order;
import com.lucky.quota.order.model.vo.OrderGoodsVO;
import com.lucky.quota.order.model.vo.OrderShopVO;
import com.lucky.quota.order.model.vo.OrderVO;
import com.lucky.quota.order.mq.OrderDeliveryProducer;
import com.lucky.quota.order.mq.OrderReceiveProducer;
import com.lucky.quota.order.service.OrderService;
import com.lucky.quota.order.service.support.OrderGlobalTransactionService;
import com.lucky.quota.shop.feign.ShopFeignClient;
import com.lucky.quota.shop.model.vo.ShopListVO;
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
    private ShopFeignClient shopFeignClient;
    @Resource
    private UserAddressFeignClient userAddressFeignClient;
    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private OrderDeliveryProducer orderDeliveryProducer;

    @Resource
    private OrderReceiveProducer orderReceiveProducer;

    @Resource
    private ActivityFeignClient activityFeignClient;

    @Resource
    private OrderGlobalTransactionService orderGlobalTransactionService;

    @Resource
    private com.lucky.quota.order.mq.OrderExpireProducer orderExpireProducer;

    /**
     * 创建申领单
     */
    @Override
    public Response<Long> addOrder(AddOrderRequest addOrderRequest) {
        if (addOrderRequest.getGoodsId() == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "物资ID不能为空");
        }
        if (addOrderRequest.getSkuId() == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU ID不能为空");
        }
        if (addOrderRequest.getBuyGoodsNum() == null || addOrderRequest.getBuyGoodsNum() <= 0) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "物资数量必须大于0");
        }
        if (addOrderRequest.getUserAddressId() == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "领取地址ID不能为空");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        Long skuId = addOrderRequest.getSkuId();
        Integer goodsNum = addOrderRequest.getBuyGoodsNum();

        // 查询物资信息
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
            log.error("==> 查询物资信息失败，goodsId: {}", addOrderRequest.getGoodsId(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询物资信息失败");
        }
        if (goods == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "物资不存在");
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
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "物资规格不存在");
        }

        // 查询领取地址
        UserAddress userAddress = null;
        try {
            Response<UserAddress> addressResponse = userAddressFeignClient.getAddressById(addOrderRequest.getUserAddressId());
            if (addressResponse != null && addressResponse.isSuccess() && addressResponse.getData() != null) {
                userAddress = addressResponse.getData();
            }
        } catch (Exception e) {
            log.error("==> 查询领取地址失败，userAddressId: {}", addOrderRequest.getUserAddressId(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询领取地址失败");
        }
        if (userAddress == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "领取地址不存在或无权使用");
        }

        assertNotInOngoingQuota(addOrderRequest.getGoodsId(), skuId);

        long orderId = IdUtil.getSnowflakeNextId();
        String orderNo = generateOrderNo(userId);
        BigDecimal totalAmount = goodsSku.getSalePrice().multiply(BigDecimal.valueOf(goodsNum));
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(OrderConstants.PAY_TIMEOUT_MINUTES);

        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .goodsId(addOrderRequest.getGoodsId())
                .shopId(goods.getShopId())
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

        orderGlobalTransactionService.lockStockAndInsertOrder(order, skuId, goodsNum);

        // 发送延迟消息，到期检查申领单是否过期（替代 OrderExpireCloseScheduler 定时扫表）
        orderExpireProducer.sendExpireCheckMessage(orderNo);

        log.info("==> 创建申领单成功，orderId: {}, orderNo: {}", orderId, orderNo);
        return Response.success(orderId);
    }

    /**
     * 用户手动取消申领单：状态置为 4（已取消），并释放预占库存。
     * 与超时自动关闭（status=6）区分。
     */
    @Override
    public Response<Boolean> cancelOrder(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单号不能为空");
        }

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单不存在");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        if (!userId.equals(order.getUserId())) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED, "无权取消该申领单");
        }

        if (OrderStatusEnum.CANCELLED.getCode().equals(order.getStatus())) {
            log.info("==> 申领单已是已取消状态，幂等返回, orderNo: {}", orderNo);
            return Response.success(true);
        }

        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            log.warn("==> 申领单不是待确认状态，无法取消，orderNo: {}, status: {}", orderNo, order.getStatus());
            throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "当前状态无法取消申领");
        }

        orderGlobalTransactionService.updateStatusAndReleaseStock(
                orderNo,
                OrderStatusEnum.CANCELLED.getCode(),
                order.getSkuId(),
                order.getBuyGoodsNum());

        log.info("==> 用户取消申领成功，orderNo: {}, status: {}", orderNo, OrderStatusEnum.CANCELLED.getCode());
        return Response.success(true);
    }

    @Override
    public Response<Boolean> confirmClaim(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单号不能为空");
        }
        return updateOrderStatus(orderNo, OrderStatusEnum.PAID.getCode());
    }

    /**
     * 查询申领单信息
     * @param orderNo 申领单号
     * @return
     */
    @Override
    public Response<Order> getOrderByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单号不能为空");
        }

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单不存在");
        }

        return Response.success(order);
    }

    @Override
    public Response<OrderVO> getOrderVOByOrderNo(String orderNo) {
        Order order = getOrderByOrderNo(orderNo).getData();
        Long userId = StpUtil.getLoginIdAsLong();
        if (!userId.equals(order.getUserId())) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED, "无权查看该申领单");
        }
        return Response.success(toOrderVO(order));
    }

    @Override
    public Response<Long> getShopIdByGoodsId(Long goodsId) {
        if (goodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "物资ID不能为空");
        }
        Goods goods = queryGoods(goodsId);
        if (goods == null) {
            throw new BizException(ResponseCodeEnum.GOODS_NOT_EXIST, "物资不存在");
        }
        return Response.success(goods.getShopId());
    }

    @Override
    public Response<Boolean> updateOrderStatus(String orderNo, Integer status) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单号不能为空");
        }
        if (status == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领状态不能为空");
        }

        if (OrderStatusEnum.CANCELLED.getCode().equals(status)) {
            return cancelOrder(orderNo);
        }

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单不存在");
        }

        if (OrderStatusEnum.PAID.getCode().equals(status)) {
            if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
                throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "当前状态无法确认领取");
            }
            orderGlobalTransactionService.deductStockAndMarkPaid(
                    orderNo, order.getSkuId(), order.getBuyGoodsNum());
            log.info("==> 更新申领状态成功，orderNo: {}, status: {}", orderNo, status);
            return Response.success(true);
        }

        int result = orderMapper.updateStatusByOrderNo(orderNo, status);
        if (result > 0) {
            log.info("==> 更新申领状态成功，orderNo: {}, status: {}", orderNo, status);
            return Response.success(true);
        }

        log.warn("==> 更新申领状态失败，orderNo: {}, status: {}", orderNo, status);
        return Response.success(false);
    }

    /**
     * 发放
     * @param orderNo 申领单号
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> deliverOrder(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单号不能为空");
        }

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.ORDER_NOT_EXIST, "申领单不存在");
        }
        if (!OrderStatusEnum.PAID.getCode().equals(order.getStatus())) {
            throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "申领状态异常，无法发放");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        Goods goods = queryGoods(order.getGoodsId());
        if (goods == null) {
            throw new BizException(ResponseCodeEnum.GOODS_NOT_EXIST, "物资不存在");
        }
        if (!userId.equals(goods.getUserId())) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED, "无权发放该申领单");
        }

        Order updateOrder = Order.builder()
                .id(order.getId())
                .status(OrderStatusEnum.SHIPPED.getCode())
                .deliveryTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        int result = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if (result <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "申领单发放失败");
        }

        LocalDateTime deliveryTime = LocalDateTime.now();
        OrderDeliveryMessage message = OrderDeliveryMessage.builder()
                .orderId(order.getId())
                .orderNo(orderNo)
                .buyerUserId(order.getUserId())
                .sellerUserId(userId)
                .goodsId(order.getGoodsId())
                .goodsName(order.getGoodsName())
                .goodsImg(order.getGoodsImg())
                .deliveryTime(deliveryTime)
                .build();

        try {
            orderDeliveryProducer.sendOrderDeliveryMessage(message);
            log.info("==> 申领单发放消息发送成功, orderNo: {}, buyerUserId: {}", orderNo, order.getUserId());
        } catch (Exception e) {
            log.warn("==> 申领单发放消息发送失败, orderNo: {}, buyerUserId: {}", orderNo, order.getUserId(), e);
        }

        log.info("==> 商家发放成功，orderNo: {}, userId: {}", orderNo, userId);
        return Response.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> confirmReceipt(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单号不能为空");
        }

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.ORDER_NOT_EXIST, "申领单不存在");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        if (!userId.equals(order.getUserId())) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED, "无权确认该申领单");
        }
        if (!OrderStatusEnum.SHIPPED.getCode().equals(order.getStatus())) {
            throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "申领状态异常，无法确认到货");
        }

        LocalDateTime receiveTime = LocalDateTime.now();
        Order updateOrder = Order.builder()
                .id(order.getId())
                .status(OrderStatusEnum.RECEIVED.getCode())
                .receiveTime(receiveTime)
                .updateTime(receiveTime)
                .build();
        int result = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if (result <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "确认到货失败");
        }

        Goods goods = queryGoods(order.getGoodsId());
        Long sellerUserId = goods != null ? goods.getUserId() : null;
        OrderReceiveMessage message = OrderReceiveMessage.builder()
                .orderId(order.getId())
                .orderNo(orderNo)
                .buyerUserId(userId)
                .sellerUserId(sellerUserId)
                .goodsId(order.getGoodsId())
                .goodsName(order.getGoodsName())
                .receiveTime(receiveTime)
                .build();

        try {
            orderReceiveProducer.sendOrderReceiveMessage(message);
            log.info("==> 申领单确认到货消息发送成功, orderNo: {}, sellerUserId: {}", orderNo, sellerUserId);
        } catch (Exception e) {
            log.warn("==> 申领单确认到货消息发送失败, orderNo: {}, sellerUserId: {}", orderNo, sellerUserId, e);
        }

        log.info("==> 用户确认到货成功，orderNo: {}, userId: {}", orderNo, userId);
        return Response.success(true);
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
        List<Order> orderList = orderMapper.selectOrdersByUserId(
                userId, request.getOrderNo(), request.getStatus(), request.getGoodsName());
        PageInfo<Order> pageInfo = new PageInfo<>(orderList);

        List<OrderVO> voList = orderList.stream()
                .map(this::toOrderVO)
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
    public Response<PageResult<OrderVO>> getMerchantOrderList(OrderQueryRequest request) {
        if (request == null) {
            request = new OrderQueryRequest();
        }
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }

        Long merchantUserId = StpUtil.getLoginIdAsLong();

        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<Order> orderList = orderMapper.selectOrdersByMerchantUserId(
                merchantUserId, request.getOrderNo(), request.getStatus(), request.getGoodsName());
        PageInfo<Order> pageInfo = new PageInfo<>(orderList);

        List<OrderVO> voList = orderList.stream()
                .map(this::toOrderVO)
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
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单ID不能为空");
        }

        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单不存在");
        }

        return Response.success(order);
    }

    @Override
    public Response<List<Order>> getOrdersByIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单ID列表不能为空");
        }

        List<Order> orders = orderMapper.selectByIds(orderIds);
        return Response.success(orders);
    }

    /**
     * 关闭过期申领单
     * @param orderNo 申领单号
     * @return
     */
    @Override
    public Response<Boolean> closeExpiredOrder(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单号不能为空");
        }

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "申领单不存在");
        }

        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            log.warn("==> 申领单不是待确认领取，无需关闭，orderNo: {}", orderNo);
            return Response.success(true);
        }

        if (order.getExpireTime() == null || LocalDateTime.now().isBefore(order.getExpireTime())) {
            log.warn("==> 申领单尚未过期，orderNo: {}", orderNo);
            return Response.success(true);
        }

        closePendingOrderAsExpired(order);
        return Response.success(true);
    }

    private void closePendingOrderAsExpired(Order order) {
        if (order == null || order.getOrderNo() == null) {
            return;
        }
        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            return;
        }
        if (order.getExpireTime() != null && LocalDateTime.now().isBefore(order.getExpireTime())) {
            return;
        }

        orderGlobalTransactionService.updateStatusAndReleaseStock(
                order.getOrderNo(),
                OrderStatusEnum.EXPIRED.getCode(),
                order.getSkuId(),
                order.getBuyGoodsNum());
        log.info("==> 超时未确认申领单已关闭，orderNo: {}, expireTime: {}", order.getOrderNo(), order.getExpireTime());
    }

    private String generateOrderNo(Long userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String userIdStr = String.format("%04d", userId % 10000);
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD" + timestamp + userIdStr + random;
    }

    private Goods queryGoods(Long goodsId) {
        if (goodsId == null) {
            return null;
        }
        try {
            Response<Goods> goodsResponse = goodsFeignClient.getGoodsById(goodsId);
            if (goodsResponse != null && goodsResponse.isSuccess()) {
                return goodsResponse.getData();
            }
        } catch (Exception e) {
            log.error("==> 查询物资信息失败，goodsId: {}", goodsId, e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询物资信息失败");
        }
        return null;
    }

    private void enrichShopId(Order order) {
        if (order == null || order.getShopId() != null || order.getGoodsId() == null) {
            return;
        }
        Goods goods = queryGoods(order.getGoodsId());
        if (goods != null) {
            order.setShopId(goods.getShopId());
        }
    }

    private OrderVO toOrderVO(Order order) {
        enrichShopId(order);
        OrderVO vo = OrderVO.fromEntity(order);
        vo.setGoods(buildOrderGoodsVO(order));
        return vo;
    }

    private OrderGoodsVO buildOrderGoodsVO(Order order) {
        if (order == null) {
            return null;
        }
        return OrderGoodsVO.builder()
                .goodsId(order.getGoodsId())
                .goodsName(order.getGoodsName())
                .goodsImg(order.getGoodsImg())
                .skuId(order.getSkuId())
                .shop(queryOrderShop(order.getShopId()))
                .build();
    }

    private OrderShopVO queryOrderShop(Long shopId) {
        if (shopId == null) {
            return null;
        }
        try {
            Response<ShopListVO> shopResponse = shopFeignClient.getShopById(shopId);
            if (shopResponse != null && shopResponse.isSuccess() && shopResponse.getData() != null) {
                ShopListVO shop = shopResponse.getData();
                return OrderShopVO.builder()
                        .shopId(shop.getId())
                        .shopName(shop.getShopName())
                        .shopAvatar(shop.getShopAvatar())
                        .shopDesc(shop.getShopDesc())
                        .build();
            }
        } catch (Exception e) {
            log.warn("==> 查询发放单位信息失败, shopId: {}", shopId, e);
        }
        return OrderShopVO.builder().shopId(shopId).build();
    }

    private void assertNotInOngoingQuota(Long goodsId, Long skuId) {
        try {
            Response<Boolean> response = activityFeignClient.isSkuInOngoingQuota(goodsId, skuId);
            if (response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData())) {
                throw new BizException(ResponseCodeEnum.QUOTA_GOODS_USE_QUOTA_CHANNEL);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("==> 查询限量场次状态失败，goodsId={}, skuId={}", goodsId, skuId, e);
        }
    }
}
