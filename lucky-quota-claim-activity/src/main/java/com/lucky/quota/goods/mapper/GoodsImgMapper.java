package com.lucky.quota.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.goods.model.entity.GoodsImg;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodsImgMapper extends BaseMapper<GoodsImg> {
    int insertSelective(GoodsImg record);

    List<GoodsImg> selectByGoodsId(Long goodsId);

    int deleteByGoodsId(@Param("goodsId") Long goodsId);
}