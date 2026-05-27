package com.lucky.seckill.pay.controller;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.pay.model.vo.PayResultVO;
import com.lucky.seckill.pay.service.PayOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay")
@Slf4j
public class PayOrderController {

    @Resource
    private PayOrderService payOrderService;


    /**
     * 查询支付状态
     * @param orderNo 订单号
     * @return 支付是否成功
     */
    @PostMapping("/query")
    public Response<Boolean> payOrder(@RequestParam("orderNo") String orderNo) {
        log.info("==> 收到查询支付状态请求, orderNo: {}", orderNo);
        return payOrderService.payOrder(orderNo);
    }

    /**
     * 支付宝异步回调
     * @param outTradeNo 商户订单号
     * @param tradeNo 支付宝交易号
     * @return success
     */
    @PostMapping("/notify")
    public String payNotify(@RequestParam("out_trade_no") String outTradeNo,
                           @RequestParam(value = "trade_no", required = false) String tradeNo) {
        log.info("==> 收到支付宝异步回调, outTradeNo: {}, tradeNo: {}", outTradeNo, tradeNo);
        try {
            payOrderService.payCallback(outTradeNo);
            return "success";
        } catch (Exception e) {
            log.error("==> 支付宝异步回调处理失败", e);
            return "fail";
        }
    }

    /**
     * 支付成功回调（支付成功后，真正扣减库存）
     * @param orderNo 订单号
     * @return true=处理成功
     */
    @PostMapping("/callback")
    public Response<Boolean> payCallback(@RequestParam("orderNo") String orderNo) {
        log.info("==> 收到支付成功回调, orderNo: {}", orderNo);
        return payOrderService.payCallback(orderNo);
    }
}
