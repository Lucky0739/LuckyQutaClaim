package com.lucky.seckill.order.controller;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.order.model.dto.AddOrderRequest;
import com.lucky.seckill.order.model.entity.Order;
import com.lucky.seckill.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 添加订单（下单）
     * @param addOrderRequest
     * @return
     */
    @PostMapping("/add")
    public Response<Long> addOrder(@RequestBody AddOrderRequest addOrderRequest) {
        log.info("==> 收到创建订单请求, goodsId: {}, skuId: {}, goodsNum: {}",
                addOrderRequest.getGoodsId(), addOrderRequest.getSkuId(), addOrderRequest.getBuyGoodsNum());
        return orderService.addOrder(addOrderRequest);
    }

    /**
     * 支付成功回调（支付成功后，真正扣减库存）
     * @param orderNo 订单号
     * @return true=处理成功
     */
    @PostMapping("/pay/callback")
    public Response<Boolean> payCallback(@RequestParam("orderNo") String orderNo) {
        log.info("==> 收到支付成功回调, orderNo: {}", orderNo);
        return orderService.paySuccess(orderNo);
    }

    /**
     * 取消订单（释放预占的库存）
     * @param orderNo 订单号
     * @return true=处理成功
     */
    @PostMapping("/cancel")
    public Response<Boolean> cancelOrder(@RequestParam("orderNo") String orderNo) {
        log.info("==> 收到取消订单请求, orderNo: {}", orderNo);
        return orderService.cancelOrder(orderNo);
    }

    /**
     * 查询订单信息
     * @param orderNo 订单号
     * @return 订单信息
     */
    @GetMapping("/query")
    public Response<Order> getOrderByOrderNo(@RequestParam("orderNo") String orderNo) {
        log.info("==> 查询订单信息, orderNo: {}", orderNo);
        return orderService.getOrderByOrderNo(orderNo);
    }

    /**
     * 更新订单状态
     * @param orderNo 订单号
     * @param status 状态
     * @return true=更新成功
     */
    @PostMapping("/updateStatus")
    public Response<Boolean> updateOrderStatus(@RequestParam("orderNo") String orderNo, @RequestParam("status") Integer status) {
        log.info("==> 更新订单状态, orderNo: {}, status: {}", orderNo, status);
        return orderService.updateOrderStatus(orderNo, status);
    }

}