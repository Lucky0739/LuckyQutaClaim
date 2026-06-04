package com.lucky.seckill.goods.mapper;

import com.lucky.seckill.goods.model.entity.GoodsImg;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodsImgMapper {
    int insertSelective(GoodsImg record);

    List<GoodsImg> selectByGoodsId(Long goodsId);

    int deleteByGoodsId(@Param("goodsId") Long goodsId);
}