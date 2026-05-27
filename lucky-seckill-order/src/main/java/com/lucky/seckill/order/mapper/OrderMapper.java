package com.lucky.seckill.order.mapper;

import com.lucky.seckill.order.model.entity.Order;
import org.apache.ibatis.annotations.Param;

public interface OrderMapper {
    int insertSelective(Order record);

    Order selectByOrderNo(@Param("orderNo") String orderNo);

    Order selectByPrimaryKey(@Param("id") Long id);

    int updateByPrimaryKeySelective(Order record);

    int updateStatusByOrderNo(@Param("orderNo") String orderNo, @Param("status") Integer status);
}
