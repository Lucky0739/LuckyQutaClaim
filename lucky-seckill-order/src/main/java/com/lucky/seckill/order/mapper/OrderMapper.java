package com.lucky.seckill.order.mapper;

import com.lucky.seckill.order.model.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper {
    int insertSelective(Order record);

    Order selectByOrderNo(@Param("orderNo") String orderNo);

    Order selectByPrimaryKey(@Param("id") Long id);

    int updateByPrimaryKeySelective(Order record);

    int updateStatusByOrderNo(@Param("orderNo") String orderNo, @Param("status") Integer status);

    /**
     * 根据用户ID查询订单列表
     * @param userId 用户ID
     * @param status 订单状态（可选）
     * @param goodsName 商品名称（可选，模糊查询）
     * @return 订单列表
     */
    List<Order> selectOrdersByUserId(@Param("userId") Long userId, 
                                     @Param("status") Integer status, 
                                     @Param("goodsName") String goodsName);

    /**
     * 批量查询订单
     * @param ids 订单ID列表
     * @return 订单列表
     */
    List<Order> selectByIds(@Param("ids") List<Long> ids);
}
