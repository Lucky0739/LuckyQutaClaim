package com.lucky.quota.goods.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.annotation.SaMode;
import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.model.dto.CategoryAddRequest;
import com.lucky.quota.goods.model.dto.CategoryUpdateRequest;
import com.lucky.quota.goods.model.entity.GoodsCategory;
import com.lucky.quota.goods.service.GoodsCategoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@Slf4j
public class GoodsCategoryController {

    @Resource
    private GoodsCategoryService goodsCategoryService;

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PostMapping("/add")
    @ApiOperationLog(description = "新增物资分类")
    public Response<Long> addCategory(@Validated @RequestBody CategoryAddRequest request) {
        log.info("==> 新增物资分类请求, categoryName: {}, parentId: {}",
                request.getCategoryName(), request.getParentId());
        return goodsCategoryService.addCategory(request);
    }

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PutMapping("/update")
    @ApiOperationLog(description = "更新物资分类")
    public Response<Void> updateCategory(@Validated @RequestBody CategoryUpdateRequest request) {
        log.info("==> 更新物资分类请求, categoryId: {}", request.getId());
        return goodsCategoryService.updateCategory(request);
    }

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @DeleteMapping("/delete/{id}")
    @ApiOperationLog(description = "删除物资分类")
    public Response<Void> deleteCategory(@PathVariable("id") Long id) {
        log.info("==> 删除物资分类请求, categoryId: {}", id);
        return goodsCategoryService.deleteCategory(id);
    }

    @SaIgnore
    @GetMapping("/{id}")
    @ApiOperationLog(description = "查询物资分类详情")
    public Response<GoodsCategory> getCategoryById(@PathVariable("id") Long id) {
        log.info("==> 查询物资分类详情请求, categoryId: {}", id);
        return goodsCategoryService.getCategoryById(id);
    }

    @SaIgnore
    @GetMapping("/list")
    @ApiOperationLog(description = "查询所有物资分类")
    public Response<List<GoodsCategory>> getAllCategory() {
        log.info("==> 查询所有物资分类请求");
        return goodsCategoryService.getAllCategory();
    }

    @SaIgnore
    @GetMapping("/parents")
    @ApiOperationLog(description = "查询父分类列表")
    public Response<List<GoodsCategory>> getParentCategories() {
        log.info("==> 查询父分类列表请求");
        return goodsCategoryService.getParentCategories();
    }

    @SaIgnore
    @GetMapping("/children/{parentId}")
    @ApiOperationLog(description = "查询子分类")
    public Response<List<GoodsCategory>> getCategoryByParentId(@PathVariable("parentId") Long parentId) {
        log.info("==> 查询子分类请求, parentId: {}", parentId);
        return goodsCategoryService.getCategoryByParentId(parentId);
    }

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PostMapping("/enable/{id}")
    @ApiOperationLog(description = "启用物资分类")
    public Response<Void> enableCategory(@PathVariable("id") Long id) {
        log.info("==> 启用物资分类请求, categoryId: {}", id);
        return goodsCategoryService.enableCategory(id);
    }

    @SaCheckRole(value = {"admin", "merchant"}, mode = SaMode.OR)
    @PostMapping("/disable/{id}")
    @ApiOperationLog(description = "禁用物资分类")
    public Response<Void> disableCategory(@PathVariable("id") Long id) {
        log.info("==> 禁用物资分类请求, categoryId: {}", id);
        return goodsCategoryService.disableCategory(id);
    }
}