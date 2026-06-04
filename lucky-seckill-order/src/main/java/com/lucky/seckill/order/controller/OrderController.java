package com.lucky.seckill.order.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.order.model.dto.AddOrderRequest;
import com.lucky.seckill.order.model.dto.OrderQueryRequest;
import com.lucky.seckill.order.model.entity.Order;
import com.lucky.seckill.order.model.vo.OrderVO;
import com.lucky.seckill.order.service.OrderService;
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
     * 添加订单（下单）
     */
    @PostMapping("/add")
    public Response<Long> addOrder(@RequestBody AddOrderRequest addOrderRequest) {
        log.info("==> 收到创建订单请求, goodsId: {}, skuId: {}, userAddressId: {}, goodsNum: {}",
                addOrderRequest.getGoodsId(), addOrderRequest.getSkuId(),
                addOrderRequest.getUserAddressId(), addOrderRequest.getBuyGoodsNum());
        return orderService.addOrder(addOrderRequest);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    public Response<Boolean> cancelOrder(@RequestParam("orderNo") String orderNo) {
        log.info("==> 收到取消订单请求, orderNo: {}", orderNo);
        return orderService.cancelOrder(orderNo);
    }

    /**
     * 查询订单信息
     */
    @GetMapping("/query")
    public Response<Order> getOrderByOrderNo(@RequestParam("orderNo") String orderNo) {
        log.info("==> 查询订单信息, orderNo: {}", orderNo);
        return orderService.getOrderByOrderNo(orderNo);
    }

    /**
     * 查询订单信息（VO格式）
     */
    @SaCheckLogin
    @GetMapping("/query/vo")
    public Response<OrderVO> getOrderVOByOrderNo(@RequestParam("orderNo") String orderNo) {
        log.info("==> 查询订单VO信息, orderNo: {}", orderNo);
        return orderService.getOrderVOByOrderNo(orderNo);
    }

    /**
     * 更新订单状态
     */
    @PostMapping("/updateStatus")
    public Response<Boolean> updateOrderStatus(@RequestParam("orderNo") String orderNo, 
                                               @RequestParam("status") Integer status) {
        log.info("==> 更新订单状态, orderNo: {}, status: {}", orderNo, status);
        return orderService.updateOrderStatus(orderNo, status);
    }

    /**
     * 商家发货
     */
    @SaCheckRole("merchant")
    @PostMapping("/deliver")
    public Response<Boolean> deliverOrder(@RequestParam("orderNo") String orderNo) {
        log.info("==> 商家发货, orderNo: {}", orderNo);
        return orderService.deliverOrder(orderNo);
    }

    /**
     * 用户确认收货
     */
    @SaCheckLogin
    @PostMapping("/receive")
    public Response<Boolean> confirmReceipt(@RequestParam("orderNo") String orderNo) {
        log.info("==> 用户确认收货, orderNo: {}", orderNo);
        return orderService.confirmReceipt(orderNo);
    }

    /**
     * 用户确认收货（PUT，便于前端调用）
     */
    @SaCheckLogin
    @PutMapping("/receive")
    public Response<Boolean> confirmReceiptByPut(@RequestParam("orderNo") String orderNo) {
        log.info("==> 用户确认收货(PUT), orderNo: {}", orderNo);
        return orderService.confirmReceipt(orderNo);
    }

    /**
     * 查询当前用户的订单列表
     */
    @SaCheckLogin
    @PostMapping("/list")
    public Response<PageResult<OrderVO>> getOrderList(@RequestBody(required = false) OrderQueryRequest request) {
        log.info("==> 查询订单列表, request: {}", request);
        return orderService.getOrderList(request);
    }

    /**
     * 商家查询自己商品的订单列表
     */
    @SaCheckRole("merchant")
    @PostMapping("/merchant/list")
    public Response<PageResult<OrderVO>> getMerchantOrderList(@RequestBody(required = false) OrderQueryRequest request) {
        log.info("==> 商家查询订单列表, request: {}", request);
        return orderService.getMerchantOrderList(request);
    }

    /**
     * 根据订单ID查询订单
     */
    @GetMapping("/get/{orderId}")
    public Response<Order> getOrderById(@PathVariable("orderId") Long orderId) {
        log.info("==> 根据订单ID查询订单, orderId: {}", orderId);
        return orderService.getOrderById(orderId);
    }

    /**
     * 根据商品ID查询店铺ID
     */
    @GetMapping("/shopId")
    public Response<Long> getShopIdByGoodsId(@RequestParam("goodsId") Long goodsId) {
        log.info("==> 根据商品ID查询店铺ID, goodsId: {}", goodsId);
        return orderService.getShopIdByGoodsId(goodsId);
    }

    /**
     * 关闭过期订单
     */
    @PostMapping("/close/expired")
    public Response<Boolean> closeExpiredOrder(@RequestParam("orderNo") String orderNo) {
        log.info("==> 关闭过期订单, orderNo: {}", orderNo);
        return orderService.closeExpiredOrder(orderNo);
    }
}
