package com.lucky.quota.goods.controller;

import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.model.dto.GoodsHomeQueryRequest;
import com.lucky.quota.goods.model.vo.GoodsListVO;
import com.lucky.quota.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页物资排行接口（独立 Controller，避免与静态资源路径冲突）
 */
@RestController
@RequestMapping("/goods/home")
@Slf4j
public class GoodsHomeController {

    @Resource
    private GoodsService goodsService;

    /**
     * 首页物资列表（Redis ZSet：综合权重 / 收藏量 / 已售数量）
     */
    @RequestMapping(value = "/list", method = {RequestMethod.GET, RequestMethod.POST})
    @ApiOperationLog(description = "首页物资列表(排行)")
    public Response<PageResult<GoodsListVO>> getHomeGoodsList(
            @RequestParam(value = "sortType", defaultValue = "weight") String sortType,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return doGetHomeGoodsList(sortType, pageNum, pageSize);
    }

    private Response<PageResult<GoodsListVO>> doGetHomeGoodsList(String sortType, Integer pageNum, Integer pageSize) {
        GoodsHomeQueryRequest request = GoodsHomeQueryRequest.builder()
                .sortType(sortType)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        log.info("==> 首页物资列表, sortType: {}, pageNum: {}, pageSize: {}", sortType, pageNum, pageSize);
        return goodsService.getHomeGoodsList(request);
    }
}
