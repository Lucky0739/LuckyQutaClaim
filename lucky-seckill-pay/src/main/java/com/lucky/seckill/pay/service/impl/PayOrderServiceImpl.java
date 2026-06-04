package com.lucky.seckill.pay.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alipay.api.AlipayApiException;
import com.lucky.seckill.common.constant.NotifyTypeConstants;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.notify.feign.NotifyFeignClient;
import com.lucky.seckill.notify.model.entity.Notify;
import com.lucky.seckill.order.enums.OrderStatusEnum;
import com.lucky.seckill.order.model.entity.Order;
import com.lucky.seckill.order.feign.OrderFeignClient;
import com.lucky.seckill.pay.model.vo.PayResultVO;
import com.lucky.seckill.pay.service.PayOrderService;
import com.lucky.seckill.pay.util.AlipayUtil;
import com.lucky.seckill.user.feign.UserBalanceFeignClient;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class PayOrderServiceImpl implements PayOrderService {

    @Resource
    private OrderFeignClient orderFeignClient;

    @Resource
    private NotifyFeignClient notifyFeignClient;

    @Resource
    private UserBalanceFeignClient userBalanceFeignClient;

    @Resource
    private AlipayUtil alipayUtil;

    /**
     * 创建沙箱支付链接
     * @param orderNo
     * @return
     */
    @Override
    public Response<PayResultVO> createSandboxPay(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }

        log.info("==> 创建沙箱支付链接，orderNo: {}", orderNo);

        try {
            Response<Order> orderResponse = orderFeignClient.getOrderByOrderNo(orderNo);
            if (orderResponse == null || !orderResponse.isSuccess() || orderResponse.getData() == null) {
                log.warn("==> 查询订单失败，orderNo: {}", orderNo);
                throw new BizException(ResponseCodeEnum.ORDER_NOT_EXIST, "订单不存在");
            }

            Order order = orderResponse.getData();
            if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
                log.warn("==> 订单状态不是待支付，orderNo: {}, status: {}", orderNo, order.getStatus());
                throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "订单状态异常");
            }

            if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
                log.warn("==> 订单已过期，orderNo: {}", orderNo);
                throw new BizException(ResponseCodeEnum.ORDER_EXPIRED, "订单已过期");
            }

            String payUrl = alipayUtil.createPayUrl(orderNo, "秒杀商品订单-" + orderNo, 
                    String.valueOf(order.getTotalAmount().setScale(2, BigDecimal.ROUND_HALF_UP)));

            PayResultVO result = PayResultVO.builder()
                    .orderNo(orderNo)
                    .payUrl(payUrl)
                    .build();

            log.info("==> 沙箱支付链接创建成功，orderNo: {}", orderNo);
            return Response.success(result);

        } catch (AlipayApiException e) {
            log.error("==> 创建沙箱支付链接失败，orderNo: {}", orderNo, e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "创建支付链接失败：" + e.getMessage());
        }
    }

    /**
     * 查询支付状态
     * @param orderNo
     * @return
     */
    @Override
    public Response<Boolean> payOrder(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }
        log.info("==> 查询支付状态，orderNo: {}", orderNo);
        try {
            Response<Order> orderResponse = orderFeignClient.getOrderByOrderNo(orderNo);
            if (orderResponse == null || !orderResponse.isSuccess() || orderResponse.getData() == null) {
                log.warn("==> 查询订单失败，orderNo: {}", orderNo);
                return Response.success(false);
            }
            Order order = orderResponse.getData();
            if (order.getStatus() == OrderStatusEnum.PAID.getCode()) {
                return Response.success(true);
            }
            return Response.success(false);
        } catch (Exception e) {
            log.error("==> 查询支付状态异常，orderNo: {}", orderNo, e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "查询支付状态异常");
        }
    }

    /**
     * 余额支付
     * @param orderNo
     * @return
     */
    @Override
    public Response<PayResultVO> balancePay(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }

        log.info("==> 发起余额支付，orderNo: {}", orderNo);
        Order order = getPendingPayOrder(orderNo);
        BigDecimal payAmount = order.getTotalAmount().setScale(2, java.math.RoundingMode.HALF_UP);

        Response<Boolean> deductResult = userBalanceFeignClient.deductBalance(order.getUserId(), payAmount);
        if (deductResult == null || !deductResult.isSuccess() || !Boolean.TRUE.equals(deductResult.getData())) {
            log.warn("==> 余额扣减失败，orderNo: {}, userId: {}, amount: {}", orderNo, order.getUserId(), payAmount);
            throw new BizException(ResponseCodeEnum.BALANCE_NOT_ENOUGH, "账户余额不足");
        }

        try {
            Response<Boolean> statusResult = orderFeignClient.updateOrderStatus(orderNo, OrderStatusEnum.PAID.getCode());
            if (statusResult == null || !statusResult.isSuccess() || !Boolean.TRUE.equals(statusResult.getData())) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "更新订单状态失败");
            }
            sendPaySuccessNotify(order);

            PayResultVO result = PayResultVO.builder()
                    .orderNo(orderNo)
                    .tradeNo("BALANCE_" + orderNo)
                    .build();
            log.info("==> 余额支付成功，orderNo: {}, userId: {}, amount: {}", orderNo, order.getUserId(), payAmount);
            return Response.success(result);
        } catch (RuntimeException e) {
            compensateBalance(order.getUserId(), payAmount, orderNo);
            throw e;
        }
    }


    /**
     * 支付宝异步回调
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> payCallback(HttpServletRequest request) {
        if (request == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "请求参数不能为空");
        }
        // 获取订单号
        String orderNo = request.getParameter("out_trade_no");

        log.info("==> 支付成功回调处理，orderNo: {}", orderNo);
        Response<Order> orderResponse = orderFeignClient.getOrderByOrderNo(orderNo);
        if (orderResponse == null || !orderResponse.isSuccess() || orderResponse.getData() == null) {
            log.error("==> 查询订单失败，orderNo: {}", orderNo);
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单不存在");
        }

        Order order = orderResponse.getData();

        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            log.warn("==> 订单状态不是待支付，orderNo: {}, status: {}", orderNo, order.getStatus());
            throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "订单状态异常");
        }

        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            log.warn("==> 订单已过期，orderNo: {}", orderNo);
            orderFeignClient.updateOrderStatus(orderNo, OrderStatusEnum.EXPIRED.getCode());
            throw new BizException(ResponseCodeEnum.ORDER_EXPIRED, "订单已过期");
        }

        Response<Boolean> statusResult = orderFeignClient.updateOrderStatus(orderNo, OrderStatusEnum.PAID.getCode());
        if (statusResult == null || !statusResult.isSuccess() || !statusResult.getData()) {
            log.error("==> 更新订单状态失败，orderNo: {}", orderNo);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "更新订单状态失败");
        }

        log.info("==> 支付回调处理成功，orderNo: {}", orderNo);

        try {
            Notify notify = Notify.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .sendUserId(5L)
                    .receiveUserId(order.getUserId())
                    .title("支付成功通知")
                    .content("您的订单【" + order.getOrderNo() + "】已支付成功，共计 ¥" + order.getTotalAmount())
                    .notifyType(NotifyTypeConstants.PAY_SUCCESS)
                    .businessId(order.getId())
                    .build();
            notifyFeignClient.saveNotify(notify);
            log.info("==> 发送支付成功通知成功，orderNo: {}", orderNo);
        } catch (Exception e) {
            log.warn("==> 发送支付成功通知失败，orderNo: {}", orderNo, e);
        }

        return Response.success(true);
    }

    /**
     * 支付成功回调（内部调用）
     * @param orderNo 订单号
     * @return true=处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> handlePaySuccess(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单号不能为空");
        }

        log.info("==> 支付成功回调处理，orderNo: {}", orderNo);
        Response<Order> orderResponse = orderFeignClient.getOrderByOrderNo(orderNo);
        if (orderResponse == null || !orderResponse.isSuccess() || orderResponse.getData() == null) {
            log.error("==> 查询订单失败，orderNo: {}", orderNo);
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "订单不存在");
        }

        Order order = orderResponse.getData();

        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            log.warn("==> 订单状态不是待支付，orderNo: {}, status: {}", orderNo, order.getStatus());
            throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "订单状态异常");
        }

        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            log.warn("==> 订单已过期，orderNo: {}", orderNo);
            orderFeignClient.updateOrderStatus(orderNo, OrderStatusEnum.EXPIRED.getCode());
            throw new BizException(ResponseCodeEnum.ORDER_EXPIRED, "订单已过期");
        }

        Response<Boolean> statusResult = orderFeignClient.updateOrderStatus(orderNo, OrderStatusEnum.PAID.getCode());
        if (statusResult == null || !statusResult.isSuccess() || !statusResult.getData()) {
            log.error("==> 更新订单状态失败，orderNo: {}", orderNo);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "更新订单状态失败");
        }

        log.info("==> 支付回调处理成功，orderNo: {}", orderNo);

        try {
            Notify notify = Notify.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .sendUserId(5L)
                    .receiveUserId(order.getUserId())
                    .title("支付成功通知")
                    .content("您的订单【" + order.getOrderNo() + "】已支付成功，共计 ¥" + order.getTotalAmount())
                    .notifyType(NotifyTypeConstants.PAY_SUCCESS)
                    .businessId(order.getId())
                    .build();
            notifyFeignClient.saveNotify(notify);
            log.info("==> 发送支付成功通知成功，orderNo: {}", orderNo);
        } catch (Exception e) {
            log.warn("==> 发送支付成功通知失败，orderNo: {}", orderNo, e);
        }

        return Response.success(true);
    }

    private Order getPendingPayOrder(String orderNo) {
        Response<Order> orderResponse = orderFeignClient.getOrderByOrderNo(orderNo);
        if (orderResponse == null || !orderResponse.isSuccess() || orderResponse.getData() == null) {
            log.error("==> 查询订单失败，orderNo: {}", orderNo);
            throw new BizException(ResponseCodeEnum.ORDER_NOT_EXIST, "订单不存在");
        }

        Order order = orderResponse.getData();
        if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            log.warn("==> 订单状态不是待支付，orderNo: {}, status: {}", orderNo, order.getStatus());
            throw new BizException(ResponseCodeEnum.ORDER_STATUS_ERROR, "订单状态异常");
        }

        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            log.warn("==> 订单已过期，orderNo: {}", orderNo);
            orderFeignClient.updateOrderStatus(orderNo, OrderStatusEnum.EXPIRED.getCode());
            throw new BizException(ResponseCodeEnum.ORDER_EXPIRED, "订单已过期");
        }
        return order;
    }

    private void sendPaySuccessNotify(Order order) {
        try {
            Notify notify = Notify.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .sendUserId(5L)
                    .receiveUserId(order.getUserId())
                    .title("支付成功通知")
                    .content("您的订单【" + order.getOrderNo() + "】已支付成功，共计 ¥" + order.getTotalAmount())
                    .notifyType(NotifyTypeConstants.PAY_SUCCESS)
                    .businessId(order.getId())
                    .build();
            notifyFeignClient.saveNotify(notify);
            log.info("==> 发送支付成功通知成功，orderNo: {}", order.getOrderNo());
        } catch (Exception e) {
            log.warn("==> 发送支付成功通知失败，orderNo: {}", order.getOrderNo(), e);
        }
    }

    private void compensateBalance(Long userId, BigDecimal amount, String orderNo) {
        try {
            Response<Boolean> compensateResult = userBalanceFeignClient.rechargeBalance(userId, amount);
            if (compensateResult == null || !compensateResult.isSuccess() || !Boolean.TRUE.equals(compensateResult.getData())) {
                log.error("==> 余额支付补偿失败，orderNo: {}, userId: {}, amount: {}", orderNo, userId, amount);
            }
        } catch (Exception e) {
            log.error("==> 余额支付补偿异常，orderNo: {}, userId: {}, amount: {}", orderNo, userId, amount, e);
        }
    }
}