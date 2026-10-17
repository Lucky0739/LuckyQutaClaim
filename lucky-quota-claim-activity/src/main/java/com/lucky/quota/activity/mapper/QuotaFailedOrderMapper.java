package com.lucky.quota.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.activity.model.entity.QuotaFailedOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 限量申领失败申领单 Mapper（人工处理用）。
 */
public interface QuotaFailedOrderMapper extends BaseMapper<QuotaFailedOrder> {

    int insertSelective(QuotaFailedOrder record);

    QuotaFailedOrder selectByOrderNo(@Param("orderNo") String orderNo);

    QuotaFailedOrder selectByPrimaryKey(@Param("id") Long id);

    /**
     * 分页查询待人工处理的失败申领单。
     *
     * @param handleStatus 处理状态（null=全部，0=待处理，1=已重试，2=已放弃）
     * @param offset       偏移量
     * @param limit        每页条数
     */
    List<QuotaFailedOrder> selectPendingPage(@Param("handleStatus") Integer handleStatus,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    /**
     * 统计待人工处理的失败申领单数。
     */
    long countPending(@Param("handleStatus") Integer handleStatus);

    /**
     * 更新处理状态（人工处理后）。
     */
    int updateHandleStatus(@Param("id") Long id,
                           @Param("handleStatus") Integer handleStatus,
                           @Param("handleRemark") String handleRemark,
                           @Param("handledBy") String handledBy);

    /**
     * 根据 orderNo 更新处理状态（幂等：只更新待处理状态的记录）。
     */
    int updateHandleStatusByOrderNo(@Param("orderNo") String orderNo,
                                     @Param("handleStatus") Integer handleStatus,
                                     @Param("handleRemark") String handleRemark,
                                     @Param("handledBy") String handledBy);
}
