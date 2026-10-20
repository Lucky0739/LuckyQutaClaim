package com.lucky.quota.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.order.model.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper extends BaseMapper<Order> {
    int insertSelective(Order record);

    Order selectByOrderNo(@Param("orderNo") String orderNo);

    Order selectByPrimaryKey(@Param("id") Long id);

    int updateByPrimaryKeySelective(Order record);

    int updateStatusByOrderNo(@Param("orderNo") String orderNo, @Param("status") Integer status);

    /**
     * 根据用户ID查询申领列表
     * @param userId 用户ID
     * @param status 申领状态（可选）
     * @param goodsName 物资名称（可选，模糊查询）
     * @return 申领列表
     */
    List<Order> selectOrdersByUserId(@Param("userId") Long userId,
                                     @Param("orderNo") String orderNo,
                                     @Param("status") Integer status,
                                     @Param("goodsName") String goodsName);

    /**
     * 商家查询自己物资产生的申领列表
     */
    List<Order> selectOrdersByMerchantUserId(@Param("merchantUserId") Long merchantUserId,
                                             @Param("orderNo") String orderNo,
                                             @Param("status") Integer status,
                                             @Param("goodsName") String goodsName);

    /**
     * 批量查询申领单
     * @param ids 申领单ID列表
     * @return 申领列表
     */
    List<Order> selectByIds(@Param("ids") List<Long> ids);
}
