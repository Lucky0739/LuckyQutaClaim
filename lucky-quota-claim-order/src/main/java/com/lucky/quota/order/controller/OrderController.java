package com.lucky.quota.order.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.lucky.quota.common.model.dto.QuotaOrderSyncRequest;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.order.model.dto.AddOrderRequest;
import com.lucky.quota.order.model.dto.OrderQueryRequest;
import com.lucky.quota.order.model.entity.Order;
import com.lucky.quota.order.model.vo.OrderVO;
import com.lucky.quota.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 创建申领单
     */
    @PostMapping("/add")
    public Response<Long> addOrder(@RequestBody AddOrderRequest addOrderRequest) {
        log.info("==> 收到创建申领单请求, goodsId: {}, skuId: {}, userAddressId: {}, goodsNum: {}",
                addOrderRequest.getGoodsId(), addOrderRequest.getSkuId(),
                addOrderRequest.getUserAddressId(), addOrderRequest.getBuyGoodsNum());
        return orderService.addOrder(addOrderRequest);
    }

    /**
     * 取消申领
     */
    @PostMapping("/cancel")
    public Response<Boolean> cancelOrder(@RequestParam("orderNo") String orderNo) {
        log.info("==> 收到取消申领请求, orderNo: {}", orderNo);
        return orderService.cancelOrder(orderNo);
    }

    /**
     * 确认领取。超时未确认由 RocketMQ 延迟消息关单。
     */
    @SaCheckLogin
    @PostMapping("/confirm")
    public Response<Boolean> confirmClaim(@RequestParam("orderNo") String orderNo) {
        log.info("==> 确认领取, orderNo: {}", orderNo);
        return orderService.confirmClaim(orderNo);
    }

    /**
     * 查询申领单信息
     */
    @GetMapping("/query")
    public Response<Order> getOrderByOrderNo(@RequestParam("orderNo") String orderNo) {
        log.info("==> 查询申领单信息, orderNo: {}", orderNo);
        return orderService.getOrderByOrderNo(orderNo);
    }

    /**
     * 查询申领单信息（VO格式）
     */
    @SaCheckLogin
    @GetMapping("/query/vo")
    public Response<OrderVO> getOrderVOByOrderNo(@RequestParam("orderNo") String orderNo) {
        log.info("==> 查询申领单VO信息, orderNo: {}", orderNo);
        return orderService.getOrderVOByOrderNo(orderNo);
    }

    /**
     * 更新申领状态
     */
    @PostMapping("/updateStatus")
    public Response<Boolean> updateOrderStatus(@RequestParam("orderNo") String orderNo, 
                                               @RequestParam("status") Integer status) {
        log.info("==> 更新申领状态, orderNo: {}, status: {}", orderNo, status);
        return orderService.updateOrderStatus(orderNo, status);
    }

    /**
     * 发放单位发放
     */
    @SaCheckRole("merchant")
    @PostMapping("/deliver")
    public Response<Boolean> deliverOrder(@RequestParam("orderNo") String orderNo) {
        log.info("==> 发放单位发放, orderNo: {}", orderNo);
        return orderService.deliverOrder(orderNo);
    }

    /**
     * 用户确认到货
     */
    @SaCheckLogin
    @PostMapping("/receive")
    public Response<Boolean> confirmReceipt(@RequestParam("orderNo") String orderNo) {
        log.info("==> 用户确认到货, orderNo: {}", orderNo);
        return orderService.confirmReceipt(orderNo);
    }

    /**
     * 用户确认到货（PUT，便于前端调用）
     */
    @SaCheckLogin
    @PutMapping("/receive")
    public Response<Boolean> confirmReceiptByPut(@RequestParam("orderNo") String orderNo) {
        log.info("==> 用户确认到货(PUT), orderNo: {}", orderNo);
        return orderService.confirmReceipt(orderNo);
    }

    /**
     * 查询当前用户的申领列表
     */
    @SaCheckLogin
    @PostMapping("/list")
    public Response<PageResult<OrderVO>> getOrderList(@RequestBody(required = false) OrderQueryRequest request) {
        log.info("==> 查询申领列表, request: {}", request);
        return orderService.getOrderList(request);
    }

    /**
     * 发放单位查询自己物资的申领列表
     */
    @SaCheckRole("merchant")
    @PostMapping("/merchant/list")
    public Response<PageResult<OrderVO>> getMerchantOrderList(@RequestBody(required = false) OrderQueryRequest request) {
        log.info("==> 发放单位查询申领列表, request: {}", request);
        return orderService.getMerchantOrderList(request);
    }

    /**
     * 根据申领单ID查询申领单
     */
    @GetMapping("/get/{orderId}")
    public Response<Order> getOrderById(@PathVariable("orderId") Long orderId) {
        log.info("==> 根据申领单ID查询申领单, orderId: {}", orderId);
        return orderService.getOrderById(orderId);
    }

    /**
     * 根据物资ID查询发放单位ID
     */
    @GetMapping("/shopId")
    public Response<Long> getShopIdByGoodsId(@RequestParam("goodsId") Long goodsId) {
        log.info("==> 根据物资ID查询发放单位ID, goodsId: {}", goodsId);
        return orderService.getShopIdByGoodsId(goodsId);
    }

    /**
     * 关闭过期申领单
     */
    @PostMapping("/close/expired")
    public Response<Boolean> closeExpiredOrder(@RequestParam("orderNo") String orderNo) {
        log.info("==> 关闭过期申领单, orderNo: {}", orderNo);
        return orderService.closeExpiredOrder(orderNo);
    }

    /**
     * 限量申领异步落库后，由活动服务镜像写入 t_order（内部 Feign，幂等）。
     */
    @PostMapping("/syncFromQuota")
    public Response<Boolean> syncFromQuota(@RequestBody QuotaOrderSyncRequest request) {
        log.info("==> 同步限量申领单到 t_order, orderNo: {}", request != null ? request.getOrderNo() : null);
        return orderService.syncFromQuota(request);
    }
}
