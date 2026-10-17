package com.lucky.quota.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.shop.model.entity.Shop;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

public interface ShopMapper extends BaseMapper<Shop> {

    int deleteByPrimaryKey(Long id);

    int insert(Shop record);

    int insertSelective(Shop record);

    Shop selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Shop record);

    int updateByPrimaryKey(Shop record);

    /**
     * 根据用户ID查询发放单位
     */
    Shop selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID统计发放单位数量
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询发放单位列表
     */
    List<Shop> selectListByUserId(@Param("userId") Long userId);

    /**
     * 查询发放单位申请列表（支持多条件筛选）
     */
    List<Shop> selectShopApplyList(
            @Param("username") String username,
            @Param("shopName") String shopName,
            @Param("auditStatus") Integer auditStatus
    );

    /**
     * 分页查询发放单位列表（支持多条件筛选）
     */
    List<Shop> selectShopList(
            @Param("username") String username,
            @Param("shopName") String shopName,
            @Param("auditStatus") Integer auditStatus,
            @Param("shopStatus") Integer shopStatus
    );

    /**
     * 更新发放单位状态
     */
    int updateShopStatus(@Param("shopId") Long shopId, @Param("shopStatus") Integer shopStatus);

    /**
     * 更新发放单位收藏数
     */
    int updateCollectNum(@Param("shopId") Long shopId, @Param("collectNum") Integer collectNum);

    /**
     * 软删除发放单位
     */
    int softDeleteById(@Param("shopId") Long shopId);
}
