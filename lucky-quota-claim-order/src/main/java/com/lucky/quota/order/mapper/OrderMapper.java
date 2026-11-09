package com.lucky.quota.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.order.model.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderMapper extends BaseMapper<Order> {
    int insertSelective(Order record);

    Order selectByOrderNo(@Param("orderNo") String orderNo);

    Order selectByPrimaryKey(@Param("id") Long id);

    int updateByPrimaryKeySelective(Order record);

    int updateStatusByOrderNo(@Param("orderNo") String orderNo, @Param("status") Integer status);

    /**
     * 支付成功入账：仅当订单仍是待支付时置为已支付并写入支付时间。
     * 返回 0 表示订单已支付/已取消/已过期，调用方需据此判断是否重复入账。
     */
    int markPaidIfPending(@Param("orderNo") String orderNo,
                          @Param("payTime") LocalDateTime payTime);

    /**
     * 带前置状态的条件更新，避免取消/过期与支付完成并发覆盖。
     */
    int updateStatusIfCurrent(@Param("orderNo") String orderNo,
                              @Param("expectedStatus") Integer expectedStatus,
                              @Param("targetStatus") Integer targetStatus);

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

    /**
     * 限量单 SK*：用 t_seckill_order 修正 t_order 的图片与时间；待支付且已过期则续 10 分钟支付窗口。
     */
    int repairQuotaOrderFromSeckill(@Param("orderNo") String orderNo);
}
