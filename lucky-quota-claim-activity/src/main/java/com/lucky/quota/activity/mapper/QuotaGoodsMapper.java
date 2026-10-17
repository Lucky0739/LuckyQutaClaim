package com.lucky.quota.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.activity.model.dto.QuotaGoodsApplyQueryRequest;
import com.lucky.quota.activity.model.entity.QuotaGoods;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuotaGoodsMapper extends BaseMapper<QuotaGoods> {

    QuotaGoods selectByPrimaryKey(Long id);

    QuotaGoods selectByActivityAndGoodsAndSku(@Param("activityId") Long activityId,
                                                @Param("goodsId") Long goodsId,
                                                @Param("skuId") Long skuId);

    QuotaGoods selectActiveByActivityAndGoodsAndSku(@Param("activityId") Long activityId,
                                                      @Param("goodsId") Long goodsId,
                                                      @Param("skuId") Long skuId);

    List<QuotaGoods> selectByActivityId(@Param("activityId") Long activityId);

    List<QuotaGoods> selectApprovedByActivityId(@Param("activityId") Long activityId);

    /**
     * 查询当前进行中限量场次内、已审核通过的限量物资（goodsId + skuId）
     */
    QuotaGoods selectOngoingApprovedByGoodsIdAndSkuId(@Param("goodsId") Long goodsId,
                                                        @Param("skuId") Long skuId,
                                                        @Param("now") LocalDateTime now);

    List<QuotaGoods> selectApplyPage(QuotaGoodsApplyQueryRequest request);

    int insertSelective(QuotaGoods record);

    int updateAuditStatus(@Param("id") Long id,
                          @Param("auditStatus") Integer auditStatus,
                          @Param("auditRemark") String auditRemark,
                          @Param("updateTime") LocalDateTime updateTime);

    int deductStock(@Param("id") Long id,
                    @Param("deductNum") Integer deductNum,
                    @Param("version") Integer version,
                    @Param("updateTime") LocalDateTime updateTime);

    int rollbackStock(@Param("id") Long id,
                      @Param("rollbackNum") Integer rollbackNum,
                      @Param("updateTime") LocalDateTime updateTime);
}
