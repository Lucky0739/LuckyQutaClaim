package com.lucky.seckill.goods.mapper;

import com.lucky.seckill.goods.model.entity.GoodsCollect;
import com.lucky.seckill.goods.model.vo.GoodsCollectVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodsCollectMapper {

    int insertSelective(GoodsCollect record);

    GoodsCollect selectByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    int deleteByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    List<GoodsCollectVO> selectCollectVOListByUserId(@Param("userId") Long userId);
}
