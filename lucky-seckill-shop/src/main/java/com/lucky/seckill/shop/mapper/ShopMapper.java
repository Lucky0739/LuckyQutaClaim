package com.lucky.seckill.shop.mapper;

import com.lucky.seckill.shop.model.entity.Shop;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.Date;
import java.util.List;

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
     * 查询商家申请列表（支持多条件筛选）
     */
    List<Shop> selectShopApplyList(
            @Param("username") String username,
            @Param("shopName") String shopName,
            @Param("auditStatus") Integer auditStatus,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime
    );
}
