package com.lucky.quota.goods.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.elasticsearch.service.GoodsElasticsearchService;
import com.lucky.quota.goods.model.dto.GoodsSearchRequest;
import com.lucky.quota.goods.model.vo.GoodsListVO;
import com.lucky.quota.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/goods/search")
@Slf4j
public class GoodsSearchController {

    @Resource
    private GoodsService goodsService;

    @Resource
    private GoodsElasticsearchService goodsElasticsearchService;

    /**
     * 物资搜索（Elasticsearch，失败时降级 MySQL 模糊查询）
     */
    @GetMapping
    @ApiOperationLog(description = "物资搜索(ES)")
    public Response<PageResult<GoodsListVO>> searchGoods(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "parentCategoryId", required = false) Long parentCategoryId,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "shopId", required = false) Long shopId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        GoodsSearchRequest request = GoodsSearchRequest.builder()
                .keyword(keyword)
                .parentCategoryId(parentCategoryId)
                .categoryId(categoryId)
                .shopId(shopId)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        log.info("==> 物资搜索, keyword: {}, categoryId: {}, shopId: {}", keyword, categoryId, shopId);
        return goodsService.searchGoods(request);
    }

    /**
     * 全量重建 ES 物资索引（管理员）
     */
    @SaCheckRole("admin")
    @PostMapping("/rebuild")
    @ApiOperationLog(description = "重建物资ES索引")
    public Response<Void> rebuildIndex() {
        log.info("==> 管理员触发物资 ES 索引重建");
        goodsElasticsearchService.rebuildIndex();
        return Response.success();
    }
}
