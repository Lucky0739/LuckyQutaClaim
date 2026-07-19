package com.lucky.seckill.shop.mapper;

import com.lucky.seckill.shop.model.entity.ShopCollect;
import com.lucky.seckill.shop.model.vo.ShopCollectVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShopCollectMapper {

    int insertSelective(ShopCollect record);

    ShopCollect selectByUserIdAndShopId(@Param("userId") Long userId, @Param("shopId") Long shopId);

    ShopCollect selectByUserIdAndShopIdIgnoreDelete(@Param("userId") Long userId, @Param("shopId") Long shopId);

    int restoreByUserIdAndShopId(@Param("userId") Long userId, @Param("shopId") Long shopId);

    int softDeleteByUserIdAndShopId(@Param("userId") Long userId, @Param("shopId") Long shopId);

    List<ShopCollectVO> selectCollectVOListByUserId(@Param("userId") Long userId);
}
