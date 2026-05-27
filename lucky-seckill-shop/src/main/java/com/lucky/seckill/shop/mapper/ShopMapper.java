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
     * 根据用户ID统计店铺数量
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询店铺列表
     */
    List<Shop> selectListByUserId(@Param("userId") Long userId);

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

    /**
     * 分页查询店铺列表（支持多条件筛选）
     */
    List<Shop> selectShopList(
            @Param("username") String username,
            @Param("shopName") String shopName,
            @Param("auditStatus") Integer auditStatus,
            @Param("shopStatus") Integer shopStatus,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime
    );

    /**
     * 更新店铺状态
     */
    int updateShopStatus(@Param("shopId") Long shopId, @Param("shopStatus") Integer shopStatus);
}
