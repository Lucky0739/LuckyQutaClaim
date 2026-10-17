package com.lucky.quota.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.shop.model.entity.ShopCollect;
import com.lucky.quota.shop.model.vo.ShopCollectVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShopCollectMapper extends BaseMapper<ShopCollect> {

    int insertSelective(ShopCollect record);

    ShopCollect selectByUserIdAndShopId(@Param("userId") Long userId, @Param("shopId") Long shopId);

    ShopCollect selectByUserIdAndShopIdIgnoreDelete(@Param("userId") Long userId, @Param("shopId") Long shopId);

    int restoreByUserIdAndShopId(@Param("userId") Long userId, @Param("shopId") Long shopId);

    int softDeleteByUserIdAndShopId(@Param("userId") Long userId, @Param("shopId") Long shopId);

    List<ShopCollectVO> selectCollectVOListByUserId(@Param("userId") Long userId);
}
