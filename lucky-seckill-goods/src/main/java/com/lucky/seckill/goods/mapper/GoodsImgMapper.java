package com.lucky.seckill.goods.mapper;

import com.lucky.seckill.goods.model.entity.GoodsImg;

import java.util.List;

public interface GoodsImgMapper {
    int insertSelective(GoodsImg record);

    List<GoodsImg> selectByGoodsId(Long goodsId);
}