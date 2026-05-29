package com.lucky.seckill.cart.mapper;

import com.lucky.seckill.cart.model.entity.Cart;
import com.lucky.seckill.cart.model.vo.CartVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CartMapper {
    int insertSelective(Cart record);

    Cart selectByUserIdAndSkuId(@Param("userId") Long userId, @Param("skuId") Long skuId);

    List<Cart> selectByUserId(@Param("userId") Long userId);

    List<CartVO> selectCartVOListByUserId(@Param("userId") Long userId);

    int updateGoodsCountByUserIdAndSkuId(@Param("userId") Long userId,
                                         @Param("skuId") Long skuId, @Param("goodsCount") Integer goodsCount);

    int deleteById(@Param("id") Long id);

    int deleteByUserIdAndSkuId(@Param("userId") Long userId, @Param("skuId") Long skuId);

    Cart selectById(@Param("id") Long id);

    int updateGoodsCountById(@Param("id") Long id, @Param("goodsCount") Integer goodsCount);

    int updateSelectedById(@Param("id") Long id, @Param("selected") Integer selected);

    int updateAllSelectedByUserId(@Param("userId") Long userId, @Param("selected") Integer selected);

    int batchDeleteByIds(@Param("userId") Long userId, @Param("cartIds") List<Long> cartIds);
}
