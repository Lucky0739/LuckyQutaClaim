package com.lucky.seckill.goods.mapper;

import com.lucky.seckill.goods.model.entity.GoodsDetail;

public interface GoodsDetailMapper {
    int insertSelective(GoodsDetail record);

    GoodsDetail selectByGoodsId(Long goodsId);
}