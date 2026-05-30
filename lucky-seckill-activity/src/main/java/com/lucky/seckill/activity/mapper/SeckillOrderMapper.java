package com.lucky.seckill.activity.mapper;

import com.lucky.seckill.activity.model.entity.SeckillOrder;
import org.apache.ibatis.annotations.Param;

public interface SeckillOrderMapper {

    int insertSelective(SeckillOrder record);

    SeckillOrder selectByOrderNo(@Param("orderNo") String orderNo);

    SeckillOrder selectByUserActivityGoodsSku(@Param("userId") Long userId,
                                              @Param("activityId") Long activityId,
                                              @Param("goodsId") Long goodsId,
                                              @Param("skuId") Long skuId);
}
