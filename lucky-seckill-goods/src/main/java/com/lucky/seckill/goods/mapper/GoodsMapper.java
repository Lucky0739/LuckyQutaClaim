package com.lucky.seckill.goods.mapper;

import com.lucky.seckill.goods.model.dto.GoodsQueryRequest;
import com.lucky.seckill.goods.model.dto.GoodsRankStatDTO;
import com.lucky.seckill.goods.model.entity.Goods;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GoodsMapper {
    int insertSelective(Goods record);

    Goods selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Goods record);

    List<Goods> selectGoodsPage(GoodsQueryRequest goodsQueryRequest);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("updateTime") LocalDateTime updateTime);

    int updateAuditStatus(@Param("id") Long id,
                          @Param("auditStatus") Integer auditStatus,
                          @Param("updateTime")LocalDateTime updateTime);

    int updateCollectNum(@Param("id") Long id,
                         @Param("collectNum") Integer collectNum,
                         @Param("updateTime") LocalDateTime updateTime);

    /**
     * 已上架且审核通过的商品排行统计（用于 ZSet 全量重建）
     */
    List<GoodsRankStatDTO> selectOnShelfGoodsRankStats();

    /**
     * 按 ID 批量查询已上架商品（首页列表）
     */
    List<Goods> selectOnShelfByIds(@Param("goodsIds") List<Long> goodsIds);

    /**
     * 可搜索商品（已上架且审核通过，用于 ES 全量重建）
     */
    List<Goods> selectSearchableGoods();
}