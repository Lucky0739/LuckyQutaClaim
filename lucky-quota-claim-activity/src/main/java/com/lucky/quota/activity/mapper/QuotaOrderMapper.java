package com.lucky.quota.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.activity.model.entity.QuotaOrder;
import org.apache.ibatis.annotations.Param;

public interface QuotaOrderMapper extends BaseMapper<QuotaOrder> {

    int insertSelective(QuotaOrder record);

    QuotaOrder selectByOrderNo(@Param("orderNo") String orderNo);

    QuotaOrder selectByUserActivityGoodsSku(@Param("userId") Long userId,
                                              @Param("activityId") Long activityId,
                                              @Param("goodsId") Long goodsId,
                                              @Param("skuId") Long skuId);
}
