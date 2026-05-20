package com.lucky.seckill.goods.mapper;

import com.lucky.seckill.goods.model.entity.Goods;

public interface GoodsMapper {
    int insertSelective(Goods record);

    Goods selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Goods record);
}