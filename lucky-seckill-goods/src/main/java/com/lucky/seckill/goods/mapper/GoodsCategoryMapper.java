package com.lucky.seckill.goods.mapper;

import com.lucky.seckill.goods.model.entity.GoodsCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodsCategoryMapper {
    int insertSelective(GoodsCategory record);

    GoodsCategory selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(GoodsCategory record);

    List<GoodsCategory> selectParentCategories();

    int deleteByPrimaryKey(Long id);

    List<GoodsCategory> selectAllCategory();


    List<GoodsCategory> selectByParentId(@Param("parentId") Long parentId);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("updateTime") java.time.LocalDateTime updateTime);

    int updateSort(@Param("id") Long id,
                   @Param("sort") Integer sort,
                   @Param("updateTime") java.time.LocalDateTime updateTime);

    int countByCategoryName(@Param("categoryName") String categoryName,
                            @Param("parentId") Long parentId);
}