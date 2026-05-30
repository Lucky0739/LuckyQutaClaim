package com.lucky.seckill.pay.controller;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.pay.model.vo.PayResultVO;
import com.lucky.seckill.pay.service.PayOrderService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pay")
@Slf4j
public class PayOrderController {

    @Resource
    private PayOrderService payOrderService;

    /**
     * 创建沙箱支付链接（跳转到支付宝沙箱支付页面）
     * @param orderNo 订单号
     * @return 支付页面HTML内容
     */
    @PostMapping("/sandbox")
    public Response<PayResultVO> createSandboxPay(@RequestParam("orderNo") String orderNo) {
        log.info("==> 收到创建沙箱支付请求, orderNo: {}", orderNo);
        return payOrderService.createSandboxPay(orderNo);
    }

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
     * @return success
     */
    @PostMapping("/notify")
    public String payCallback(HttpServletRequest request) {
        log.info("==> 收到支付宝异步回调"+request);
        try {
            payOrderService.payCallback(request);
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
    public Response<Boolean> handlePaySuccess(@RequestParam("orderNo") String orderNo) {
        log.info("==> 收到支付成功回调, orderNo: {}", orderNo);
        return payOrderService.handlePaySuccess(orderNo);
    }
}
