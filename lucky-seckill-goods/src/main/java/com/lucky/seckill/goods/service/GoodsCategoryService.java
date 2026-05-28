package com.lucky.seckill.goods.service;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.model.dto.CategoryAddRequest;
import com.lucky.seckill.goods.model.dto.CategoryUpdateRequest;
import com.lucky.seckill.goods.model.entity.GoodsCategory;

import java.util.List;

public interface GoodsCategoryService {
    Response<Long> addCategory(CategoryAddRequest request);

    Response<Void> updateCategory(CategoryUpdateRequest request);

    Response<Void> deleteCategory(Long id);

    Response<GoodsCategory> getCategoryById(Long id);

    Response<List<GoodsCategory>> getAllCategory();

    Response<List<GoodsCategory>> getParentCategories();

    Response<List<GoodsCategory>> getCategoryByParentId(Long parentId);

    Response<Void> enableCategory(Long id);

    Response<Void> disableCategory(Long id);

}