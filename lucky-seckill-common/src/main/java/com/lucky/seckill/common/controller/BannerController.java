
package com.lucky.seckill.common.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import com.lucky.seckill.common.aspect.ApiOperationLog;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.model.request.BannerRequest;
import com.lucky.seckill.common.model.request.BannerSortRequest;
import com.lucky.seckill.common.service.BannerService;
import com.lucky.seckill.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/banner")
@Slf4j
public class BannerController {

    @Resource
    private BannerService bannerService;

    @SaCheckRole("admin")
    @PostMapping("/img/upload")
    @ApiOperationLog(description = "上传单张背景图")
    public Response<UploadPictureResult> uploadBannerImg(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("==> 上传背景图失败，文件为空");
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "请上传图片");
        }
        log.info("==> 上传单张背景图请求");
        return bannerService.uploadBannerImg(file);
    }

    @SaCheckRole("admin")
    @PostMapping("/img/save-urls")
    @ApiOperationLog(description = "保存背景图URL数组")
    public Response<List<String>> saveBannerImgUrls(@RequestBody List<BannerRequest> bannerRequests) {
        if (bannerRequests == null || bannerRequests.isEmpty()) {
            log.warn("==> 保存背景图URL数组失败，URL数组为空");
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "请提供图片URL数组，不能为空");
        }

        log.info("==> 保存背景图URL数组请求, 数量: {}, Requests: {}", bannerRequests.size(), bannerRequests);

        return bannerService.saveBannerImgUrls(bannerRequests);
    }


    @SaIgnore
    @GetMapping("/list")
    @ApiOperationLog(description = "查询轮播图列表")
    public Response<List<String>> getBannerList() {
        log.info("==> 查询轮播图列表请求");
        return bannerService.getBannerList();
    }

    @SaCheckRole("admin")
    @DeleteMapping("/{bannerId}")
    @ApiOperationLog(description = "删除单个轮播图")
    public Response<?> deleteBanner(@PathVariable Long bannerId) {
        if (bannerId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "轮播图ID不能为空");
        }
        log.info("==> 删除轮播图请求, bannerId: {}", bannerId);
        return bannerService.deleteBanner(bannerId);
    }

    @SaCheckRole("admin")
    @PostMapping("/sort/update")
    @ApiOperationLog(description = "更新轮播图排序")
    public Response<?> updateBannerSort(@RequestBody List<BannerSortRequest> sortRequests) {
        if (sortRequests == null || sortRequests.isEmpty()) {
            log.warn("==> 更新轮播图排序失败，排序请求为空");
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "排序请求不能为空");
        }
        log.info("==> 更新轮播图排序请求, 数量: {}", sortRequests.size());
        return bannerService.updateBannerSort(sortRequests);
    }
}
