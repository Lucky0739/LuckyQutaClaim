package com.lucky.seckill.goods.mapper;

import com.lucky.seckill.goods.model.dto.GoodsQueryRequest;
import com.lucky.seckill.goods.model.entity.Goods;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodsMapper {
    int insertSelective(Goods record);

    Goods selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Goods record);

    List<Goods> selectGoodsPage(GoodsQueryRequest goodsQueryRequest);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("updateTime") java.time.LocalDateTime updateTime);
}