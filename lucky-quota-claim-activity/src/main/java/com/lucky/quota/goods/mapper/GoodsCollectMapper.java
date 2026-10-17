package com.lucky.quota.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.goods.model.entity.GoodsCollect;
import com.lucky.quota.goods.model.vo.GoodsCollectVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodsCollectMapper extends BaseMapper<GoodsCollect> {

    int insertSelective(GoodsCollect record);

    GoodsCollect selectByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    int deleteByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    List<GoodsCollectVO> selectCollectVOListByUserId(@Param("userId") Long userId);
}
