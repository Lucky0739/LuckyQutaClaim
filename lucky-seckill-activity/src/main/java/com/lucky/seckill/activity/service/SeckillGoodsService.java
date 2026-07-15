package com.lucky.seckill.activity.service;

import com.lucky.seckill.activity.model.dto.SeckillGoodsApplyQueryRequest;
import com.lucky.seckill.activity.model.dto.SeckillGoodsApplyRequest;
import com.lucky.seckill.activity.model.dto.SeckillGoodsAuditRequest;
import com.lucky.seckill.activity.model.vo.SeckillGoodsApplyVO;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.utils.Response;

public interface SeckillGoodsService {

    Response<Long> applySeckillGoods(SeckillGoodsApplyRequest request);

    Response<Void> auditSeckillGoods(SeckillGoodsAuditRequest request);

    Response<PageResult<SeckillGoodsApplyVO>> listApplyPage(SeckillGoodsApplyQueryRequest request);

    Response<SeckillGoodsApplyVO> getSeckillGoodsDetail(Long seckillGoodsId);
}
