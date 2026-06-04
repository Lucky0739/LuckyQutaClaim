package com.lucky.seckill.goods.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.model.dto.GoodsAuditRequest;
import com.lucky.seckill.goods.model.dto.GoodsQueryRequest;
import com.lucky.seckill.goods.model.vo.GoodsListVO;
import com.lucky.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理员控制器
 * 用于管理员审核商品上架申请
 */
@RestController
@RequestMapping("/goods/admin")
@Slf4j
public class GoodsAdminController {

    @Resource
    private GoodsService goodsService;

    /**
     * 管理员审核商品上架申请
     * @param request 审核请求
     * @return
     */
    @SaCheckRole("admin")
    @PostMapping("/audit")
    @ApiOperationLog(description = "管理员审核商品上架申请")
    public Response<Void> auditGoods(@Valid @RequestBody GoodsAuditRequest request) {
        log.info("==> 管理员审核商品上架申请, goodsId: {}, auditStatus: {}, auditRemark: {}",
                request.getGoodsId(), request.getAuditStatus(), request.getAuditRemark());
        return goodsService.auditGoods(request);
    }

    /**
     * 查询待审核商品列表
     * @param queryRequest 查询条件
     * @return 待审核商品分页列表
     */
    @SaCheckRole("admin")
    @PostMapping("/pendingAudit/page")
    @ApiOperationLog(description = "查询待审核商品列表")
    public Response<PageResult<GoodsListVO>> getPendingAuditGoodsPage(@RequestBody GoodsQueryRequest queryRequest) {
        log.info("==> 管理员查询待审核商品列表, 查询条件: {}", queryRequest);

        return goodsService.getPendingAuditGoodsPage(queryRequest);
    }

    /**
     * 查询已审核商品列表（包含通过和拒绝）
     * @param queryRequest 查询条件
     * @return 已审核商品分页列表
     */
    @SaCheckRole("admin")
    @PostMapping("/audited/page")
    @ApiOperationLog(description = "查询已审核商品列表")
    public Response<PageResult<GoodsListVO>> getAuditedGoodsPage(@RequestBody GoodsQueryRequest queryRequest) {
        log.info("==> 管理员查询已审核商品列表, 查询条件: {}", queryRequest);

        return goodsService.getAuditedGoodsPage(queryRequest);
    }
}
