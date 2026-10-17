package com.lucky.quota.goods.service.impl;

import cn.hutool.core.util.IdUtil;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.mapper.GoodsCategoryMapper;
import com.lucky.quota.goods.model.dto.CategoryAddRequest;
import com.lucky.quota.goods.model.dto.CategoryUpdateRequest;
import com.lucky.quota.goods.model.entity.GoodsCategory;
import com.lucky.quota.goods.service.GoodsCategoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class GoodsCategoryServiceImpl implements GoodsCategoryService {

    @Resource
    private GoodsCategoryMapper goodsCategoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> addCategory(CategoryAddRequest request) {
        String categoryName = request.getCategoryName();
        Long parentId = request.getParentId();

        Integer count = goodsCategoryMapper.countByCategoryName(categoryName, parentId);
        if (count != null && count > 0) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "同一分类下已存在相同名称的分类");
        }

        Long categoryId = IdUtil.getSnowflakeNextId();
        GoodsCategory category = GoodsCategory.builder()
                .id(categoryId)
                .categoryName(categoryName)
                .parentId(parentId)
                .sort(request.getSort() != null ? request.getSort() : 0)
                .status(1)
                .isDelete(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        goodsCategoryMapper.insertSelective(category);
        log.info("==> 新增物资分类成功, categoryId: {}, categoryName: {}", categoryId, categoryName);
        return Response.success(categoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> updateCategory(CategoryUpdateRequest request) {
        Long id = request.getId();
        if (id == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "分类ID不能为空");
        }

        GoodsCategory existingCategory = goodsCategoryMapper.selectByPrimaryKey(id);
        if (existingCategory == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "分类不存在");
        }

        GoodsCategory updateCategory = GoodsCategory.builder()
                .id(id)
                .updateTime(LocalDateTime.now())
                .build();

        if (request.getCategoryName() != null && !request.getCategoryName().isEmpty()) {
            if (!request.getCategoryName().equals(existingCategory.getCategoryName())) {
                Integer count = goodsCategoryMapper.countByCategoryName(request.getCategoryName(),
                        request.getParentId() != null ? request.getParentId() : existingCategory.getParentId());
                if (count != null && count > 0) {
                    throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "同一分类下已存在相同名称的分类");
                }
            }
            updateCategory.setCategoryName(request.getCategoryName());
        }

        if (request.getParentId() != null) {
            updateCategory.setParentId(request.getParentId());
        }

        if (request.getSort() != null) {
            updateCategory.setSort(request.getSort());
        }

        if (request.getStatus() != null) {
            updateCategory.setStatus(request.getStatus());
        }

        goodsCategoryMapper.updateByPrimaryKeySelective(updateCategory);
        log.info("==> 更新物资分类成功, categoryId: {}", id);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> deleteCategory(Long id) {
        if (id == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "分类ID不能为空");
        }

        GoodsCategory category = goodsCategoryMapper.selectByPrimaryKey(id);
        if (category == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "分类不存在");
        }

        List<GoodsCategory> children = goodsCategoryMapper.selectByParentId(id);
        if (children != null && !children.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "该分类下存在子分类，无法删除");
        }

        goodsCategoryMapper.deleteByPrimaryKey(id);
        log.info("==> 删除物资分类成功, categoryId: {}", id);
        return Response.success();
    }

    @Override
    public Response<GoodsCategory> getCategoryById(Long id) {
        if (id == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "分类ID不能为空");
        }

        GoodsCategory category = goodsCategoryMapper.selectByPrimaryKey(id);
        if (category == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "分类不存在");
        }

        log.info("==> 查询物资分类成功, categoryId: {}", id);
        return Response.success(category);
    }

    @Override
    public Response<List<GoodsCategory>> getAllCategory() {
        List<GoodsCategory> categories = goodsCategoryMapper.selectAllCategory();
        log.info("==> 查询所有物资分类成功, count: {}", categories != null ? categories.size() : 0);
        return Response.success(categories);
    }

    @Override
    public Response<List<GoodsCategory>> getCategoryByParentId(Long parentId) {
        if (parentId == null) {
            parentId = 0L;
        }
        List<GoodsCategory> categories = goodsCategoryMapper.selectByParentId(parentId);
        log.info("==> 查询物资子分类成功, parentId: {}, count: {}", parentId, categories != null ? categories.size() : 0);
        return Response.success(categories);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> enableCategory(Long id) {
        if (id == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "分类ID不能为空");
        }

        GoodsCategory category = goodsCategoryMapper.selectByPrimaryKey(id);
        if (category == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "分类不存在");
        }

        if (category.getStatus() == 1) {
            log.warn("==> 分类已处于启用状态, categoryId: {}", id);
            return Response.success();
        }

        goodsCategoryMapper.updateStatus(id, 1, LocalDateTime.now());
        log.info("==> 启用物资分类成功, categoryId: {}", id);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> disableCategory(Long id) {
        if (id == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "分类ID不能为空");
        }

        GoodsCategory category = goodsCategoryMapper.selectByPrimaryKey(id);
        if (category == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "分类不存在");
        }

        if (category.getStatus() == 0) {
            log.warn("==> 分类已处于禁用状态, categoryId: {}", id);
            return Response.success();
        }

        goodsCategoryMapper.updateStatus(id, 0, LocalDateTime.now());
        log.info("==> 禁用物资分类成功, categoryId: {}", id);
        return Response.success();
    }

    @Override
    public Response<List<GoodsCategory>> getParentCategories() {
        List<GoodsCategory> categories = goodsCategoryMapper.selectParentCategories();
        log.info("==> 查询父分类列表成功, count: {}", categories != null ? categories.size() : 0);
        return Response.success(categories);
    }
}