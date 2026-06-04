package com.lucky.seckill.pay.service;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.pay.model.vo.PayResultVO;
import jakarta.servlet.http.HttpServletRequest;

public interface PayOrderService {

    Response<PayResultVO> createSandboxPay(String orderNo);

    Response<Boolean> payOrder(String orderNo);

    Response<PayResultVO> balancePay(String orderNo);

    Response<Boolean> payCallback(HttpServletRequest request);

    Response<Boolean> handlePaySuccess(String orderNo);
}
