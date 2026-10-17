package com.lucky.quota.shop.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.shop.model.dto.ShopApplyQueryRequest;
import com.lucky.quota.shop.model.dto.ShopAuditRequest;
import com.lucky.quota.shop.model.vo.ShopApplyVO;
import com.lucky.quota.shop.service.ShopService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家管理员控制器
 * 用于管理员审批商家申请
 */
@RestController
@RequestMapping("/shop/admin")
@Slf4j
public class ShopAdminController {

    @Resource
    private ShopService shopService;

    /**
     * 管理员审批商家申请
     * @param shopAuditRequest
     * @return
     */
    @SaCheckRole("admin")
    @PostMapping("/audit")
    public Response<?> auditMerchantApply(@Validated @RequestBody ShopAuditRequest shopAuditRequest) {
        log.info("==> [DEBUG] 当前登录用户ID: {}", StpUtil.getLoginId());
        log.info("==> [DEBUG] 当前用户角色列表: {}", StpUtil.getRoleList());
        log.info("==> 管理员审批商家申请, adminId: {}, shopId: {}, auditStatus: {}",
                StpUtil.getLoginId(), shopAuditRequest.getShopId(), shopAuditRequest.getAuditStatus());
        return shopService.auditShop(shopAuditRequest);
    }

    /**
     * 查询发放单位申请列表（支持多条件筛选和分页）
     * @param queryRequest 查询条件
     * @return 分页结果
     */
    @SaCheckRole("admin")
    @PostMapping("/apply/query")
    public Response<?> getShopApplyList(@Validated @RequestBody ShopApplyQueryRequest queryRequest) {
        log.info("==> 管理员查询发放单位申请列表, adminId: {}, 查询条件: {}",
                StpUtil.getLoginId(), queryRequest);
        return shopService.getShopApplyList(queryRequest);
    }

    /**
     * 管理员分页查询发放单位列表（支持多条件筛选）
     * @param queryRequest 查询条件
     * @return 分页结果
     */
    @SaCheckRole("admin")
    @PostMapping("/list")
    public Response<PageResult<ShopApplyVO>> getShopList(@Validated @RequestBody ShopApplyQueryRequest queryRequest) {
        log.info("==> 管理员查询发放单位列表, adminId: {}, 查询条件: {}",
                StpUtil.getLoginId(), queryRequest);
        return shopService.getShopList(queryRequest);
    }

    /**
     * 禁用/启用发放单位
     * @param shopId 发放单位ID
     * @param shopStatus 发放单位状态 0-禁用 1-启用
     */
    @SaCheckRole("admin")
    @PostMapping("/status/update")
    public Response<?> updateShopStatus(@RequestParam("shopId") Long shopId,
                                         @RequestParam("shopStatus") Integer shopStatus) {
        log.info("==> 管理员禁用/启用发放单位, adminId: {}, shopId: {}, shopStatus: {}",
                StpUtil.getLoginId(), shopId, shopStatus);
        return shopService.updateShopStatus(shopId, shopStatus);
    }

}
