package com.lucky.quota.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.goods.model.entity.GoodsDetail;
import org.apache.ibatis.annotations.Param;

public interface GoodsDetailMapper extends BaseMapper<GoodsDetail> {
    int insertSelective(GoodsDetail record);

    GoodsDetail selectByGoodsId(Long goodsId);

    int updateContentByGoodsId(@Param("goodsId") Long goodsId,
                               @Param("detailContent") String detailContent,
                               @Param("updateTime") java.time.LocalDateTime updateTime);
}