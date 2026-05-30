package com.lucky.seckill.activity.service;

import com.lucky.seckill.activity.model.dto.SeckillRequest;
import com.lucky.seckill.activity.model.vo.SeckillResultVO;
import com.lucky.seckill.common.utils.Response;

public interface SeckillService {

    Response<SeckillResultVO> doSeckill(SeckillRequest request);
}
