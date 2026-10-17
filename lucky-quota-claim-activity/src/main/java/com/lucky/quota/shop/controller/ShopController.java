package com.lucky.quota.shop.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import com.lucky.quota.common.aspect.ApiOperationLog;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.model.file.UploadPictureResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.model.vo.GoodsListVO;
import com.lucky.quota.shop.model.dto.CreateShopRequest;
import com.lucky.quota.shop.model.dto.ShopApplyQueryRequest;
import com.lucky.quota.shop.model.dto.ShopAuditRequest;
import com.lucky.quota.shop.model.vo.ShopApplyVO;
import com.lucky.quota.shop.model.vo.ShopListVO;
import com.lucky.quota.shop.service.ShopService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 发放单位控制器
 */
@RestController
@RequestMapping("/shop")
@Slf4j
public class ShopController {
    @Resource
    private ShopService shopService;

    /**
     * 上传单位头像（申请发放单位前上传，将返回的 imageUrl 填入发放单位申请的 shopAvatar 字段）
     */
    @SaCheckLogin
    @PostMapping("/avatar/upload")
    @ApiOperationLog(description = "上传单位头像")
    public Response<UploadPictureResult> uploadShopAvatar(@RequestParam("file") MultipartFile file) {
        return shopService.uploadShopAvatar(file);
    }

    @SaCheckLogin
    @PostMapping("/create")
    public Response<?> createShop(@Validated @RequestBody CreateShopRequest request) {
        log.info("==> 接收到申请发放单位请求, shopName: {}", request.getShopName());
        return shopService.createShop(request);
    }

    @PostMapping("/audit")
    public Response<?> auditShop(@Validated @RequestBody ShopAuditRequest request) {
        log.info("==> 接收到发放单位审批请求, shopId: {}, auditStatus: {}",
                request.getShopId(), request.getAuditStatus());
        return shopService.auditShop(request);
    }

    /**
     * 分页查询发放单位列表（公开访问）
     */
    @SaIgnore
    @PostMapping("/list")
    @ApiOperationLog(description = "分页查询发放单位列表")
    public Response<PageResult<ShopListVO>> getShopList(@RequestBody(required = false) ShopApplyQueryRequest queryRequest) {
        if (queryRequest == null) {
            queryRequest = new ShopApplyQueryRequest();
        }
        queryRequest.setAuditStatus(1);
        queryRequest.setShopStatus(1);
        log.info("==> 公开查询发放单位列表, 查询条件: {}", queryRequest);
        return shopService.getPublicShopList(queryRequest);
    }

    /**
     * 商家查看自己的发放单位列表
     */
    @SaCheckLogin
    @GetMapping("/my/list")
    @ApiOperationLog(description = "商家查看自己的发放单位列表")
    public Response<PageResult<ShopApplyVO>> getMyShopList() {
        log.info("==> 商家查看自己的发放单位列表请求");
        return shopService.getMyShopList();
    }

    /**
     * 商家删除自己的发放单位
     */
    @SaCheckLogin
    @DeleteMapping("/my/{shopId}")
    @ApiOperationLog(description = "商家删除自己的发放单位")
    public Response<?> deleteMyShop(@PathVariable("shopId") Long shopId) {
        log.info("==> 商家删除自己的发放单位, shopId: {}", shopId);
        return shopService.deleteMyShop(shopId);
    }

    /**
     * 根据发放单位ID查询发放单位信息（公开访问）
     */
    @SaIgnore
    @GetMapping("/{shopId}")
    @ApiOperationLog(description = "根据发放单位ID查询发放单位信息")
    public Response<ShopListVO> getShopById(@PathVariable("shopId") Long shopId) {
        log.info("==> 根据发放单位ID查询发放单位信息, shopId: {}", shopId);
        return shopService.getShopById(shopId);
    }

    /**
     * 按发放单位ID查询发放单位物资列表（公开访问）
     */
    @SaIgnore
    @GetMapping("/{shopId}/goods")
    @ApiOperationLog(description = "按发放单位ID查询物资列表")
    public Response<PageResult<GoodsListVO>> getShopGoods(@PathVariable("shopId") Long shopId,
                                                          @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("==> 按发放单位ID查询物资, shopId: {}, pageNum: {}, pageSize: {}", shopId, pageNum, pageSize);
        return shopService.getShopGoods(shopId, pageNum, pageSize);
    }

}
