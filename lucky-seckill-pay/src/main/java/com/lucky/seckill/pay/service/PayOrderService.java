package com.lucky.seckill.pay.service;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.pay.model.vo.PayResultVO;

public interface PayOrderService {


    Response<Boolean> payOrder(String orderNo);

    Response<Boolean> payCallback(String orderNo);
}
