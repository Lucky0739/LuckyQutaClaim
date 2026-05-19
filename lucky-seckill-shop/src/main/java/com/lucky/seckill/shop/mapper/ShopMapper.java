package com.lucky.seckill.shop.mapper;

import com.lucky.seckill.shop.model.entity.Shop;
import io.lettuce.core.dynamic.annotation.Param;

public interface ShopMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Shop record);

    int insertSelective(Shop record);

    Shop selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Shop record);

    int updateByPrimaryKey(Shop record);

    /**
     * 根据用户ID查询店铺
     */
    Shop selectByUserId(@Param("userId") Long userId);

    /**
     * 查询待审核的店铺列表
     */
    java.util.List<Shop> selectPendingAuditList();
}
