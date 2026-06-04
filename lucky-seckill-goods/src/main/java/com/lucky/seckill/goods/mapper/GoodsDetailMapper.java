package com.lucky.seckill.goods.mapper;

import com.lucky.seckill.goods.model.entity.GoodsDetail;
import org.apache.ibatis.annotations.Param;

public interface GoodsDetailMapper {
    int insertSelective(GoodsDetail record);

    GoodsDetail selectByGoodsId(Long goodsId);

    int updateContentByGoodsId(@Param("goodsId") Long goodsId,
                               @Param("detailContent") String detailContent,
                               @Param("updateTime") java.time.LocalDateTime updateTime);
}