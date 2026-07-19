package com.lucky.seckill.activity.service;

import com.lucky.seckill.activity.model.dto.SeckillRequest;
import com.lucky.seckill.activity.model.vo.SeckillResultVO;
import com.lucky.seckill.common.utils.Response;

public interface SeckillService {

    /**
     * 执行秒杀
     */
    Response<SeckillResultVO> doSeckill(SeckillRequest request);

    /**
     * 查询秒杀订单异步处理结果（前端轮询用）。
     * <p>
     * 返回值 status：-1=处理中，0=待支付（创建成功），-2=创建失败。
     * Redis key 过期（超过30分钟）返回 null → 前端展示"订单创建超时"。
     */
    Response<SeckillResultVO> getSeckillResult(String orderNo);
}
