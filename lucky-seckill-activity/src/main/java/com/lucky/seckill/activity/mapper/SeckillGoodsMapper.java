package com.lucky.seckill.activity.mapper;

import com.lucky.seckill.activity.model.dto.SeckillGoodsApplyQueryRequest;
import com.lucky.seckill.activity.model.entity.SeckillGoods;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeckillGoodsMapper {

    SeckillGoods selectByPrimaryKey(Long id);

    SeckillGoods selectByActivityAndGoodsAndSku(@Param("activityId") Long activityId,
                                                @Param("goodsId") Long goodsId,
                                                @Param("skuId") Long skuId);

    SeckillGoods selectActiveByActivityAndGoodsAndSku(@Param("activityId") Long activityId,
                                                      @Param("goodsId") Long goodsId,
                                                      @Param("skuId") Long skuId);

    List<SeckillGoods> selectByActivityId(@Param("activityId") Long activityId);

    List<SeckillGoods> selectApprovedByActivityId(@Param("activityId") Long activityId);

    List<SeckillGoods> selectApplyPage(SeckillGoodsApplyQueryRequest request);

    int insertSelective(SeckillGoods record);

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
