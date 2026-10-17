package com.lucky.quota.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.goods.model.entity.GoodsSku;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodsSkuMapper extends BaseMapper<GoodsSku> {
    int insertSelective(GoodsSku record);

    List<GoodsSku> selectByGoodsId(Long goodsId);

    GoodsSku selectById(Long id);

    /**
     * 预占库存（使用行锁）
     * @param skuId SKU ID
     * @param buyGoodsNum 预占数量
     * @return 影响的行数，0表示库存不足
     */
    int lockStock(@Param("skuId") Long skuId, @Param("buyGoodsNum") Integer buyGoodsNum);

    /**
     * 真正扣减库存（使用行锁）
     * @param skuId SKU ID
     * @param stockNum 扣减数量
     * @return 影响的行数，0表示库存不足
     */
    int deductStock(@Param("skuId") Long skuId, @Param("stockNum") Integer stockNum);

    /**
     * 释放预占的库存（申领单取消或超时）
     * @param skuId SKU ID
     * @param stockNum 释放数量
     * @return 影响的行数
     */
    int releaseStock(@Param("skuId") Long skuId, @Param("stockNum") Integer stockNum);

    /**
     * 增加已售数量
     * @param skuId SKU ID
     * @param salesNum 销售数量
     * @return 影响的行数
     */
    int increaseSalesNum(@Param("skuId") Long skuId, @Param("salesNum") Integer salesNum);

    /**
     * 统计物资下所有 SKU 已售数量之和
     */
    Integer sumSalesNumByGoodsId(@Param("goodsId") Long goodsId);

    int updateByPrimaryKeySelective(GoodsSku record);

    int softDeleteById(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}