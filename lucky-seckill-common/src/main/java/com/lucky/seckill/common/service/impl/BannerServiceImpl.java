
package com.lucky.seckill.common.service.impl;

import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.manager.upload.FilePictureUpload;
import com.lucky.seckill.common.mapper.BannerMapper;
import com.lucky.seckill.common.model.entity.Banner;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.model.request.BannerRequest;
import com.lucky.seckill.common.model.request.BannerSortRequest;
import com.lucky.seckill.common.service.BannerService;
import com.lucky.seckill.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class BannerServiceImpl implements BannerService {

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private BannerMapper bannerMapper;

    @Override
    public Response<UploadPictureResult> uploadBannerImg(MultipartFile file) {
        UploadPictureResult uploadResult = filePictureUpload.uploadPicture(file, "banner", true);
        log.info("==> 背景图上传成功, imgUrl: {}", uploadResult.getImageUrl());
        return Response.success(uploadResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<List<String>> saveBannerImgUrls(List<BannerRequest> bannerRequests) {
        if (bannerRequests == null || bannerRequests.isEmpty()) {
            log.warn("==> 保存背景图URL数组失败，URL数组为空");
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "请提供图片URL");
        }

        List<String> validUrls = new ArrayList<>();
        for (BannerRequest request : bannerRequests) {
            if (request.getBannerUrl() == null || request.getBannerUrl().trim().isEmpty()) {
                log.warn("==> 无效的图片URL: {}", request.getBannerUrl());
                throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "图片URL不能为空");
            }
            String trimmed = request.getBannerUrl().trim();
            if (!isValidImageUrl(trimmed)) {
                log.warn("==> 无效的图片URL: {}", trimmed);
                throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "图片URL格式不正确: " + trimmed);
            }
            if (request.getSort() == null) {
                log.warn("==> sort不能为空");
                throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "排序值不能为空");
            }
            validUrls.add(trimmed);
        }

        if (validUrls.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "没有有效的图片URL");
        }

        bannerMapper.deleteAllBanners();

        List<Banner> banners = new ArrayList<>();
        for (BannerRequest request : bannerRequests) {
            Banner banner = Banner.builder()
                    .bannerUrl(request.getBannerUrl().trim())
                    .sort(request.getSort())
                    .status(1)
                    .isDelete(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            banners.add(banner);
        }

        int result = bannerMapper.batchInsert(banners);
        if (result != bannerRequests.size()) {
            log.error("==> 批量插入轮播图失败, 预期: {}, 实际: {}", bannerRequests.size(), result);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "保存轮播图失败");
        }

        log.info("==> 背景图URL数组保存成功, 总数: {}", bannerRequests.size());
        return Response.success(validUrls);
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        return lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://") || lowerUrl.startsWith("/");
    }
    @Override
    public Response<List<String>> getBannerList() {
        log.info("==> 查询轮播图列表请求");

        List<Banner> banners = bannerMapper.selectAllBanners();

        if (banners == null || banners.isEmpty()) {
            log.info("==> 轮播图列表为空");
            return Response.success(new ArrayList<>());
        }

        List<String> bannerUrls = new ArrayList<>();
        for (Banner banner : banners) {
            if (banner.getBannerUrl() != null && !banner.getBannerUrl().trim().isEmpty()) {
                bannerUrls.add(banner.getBannerUrl());
            }
        }

        log.info("==> 查询轮播图列表成功, 数量: {}", bannerUrls.size());
        return Response.success(bannerUrls);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteBanner(Long bannerId) {
        log.info("==> 删除轮播图请求, bannerId: {}", bannerId);

        Banner banner = bannerMapper.selectByPrimaryKey(bannerId);
        if (banner == null || banner.getIsDelete() == 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "轮播图不存在");
        }

        bannerMapper.deleteByPrimaryKey(bannerId);

        reorderBanners();

        log.info("==> 删除轮播图成功, bannerId: {}", bannerId);
        return Response.success();
    }

    private void reorderBanners() {
        List<Banner> activeBanners = bannerMapper.selectAllBanners();

        if (activeBanners == null || activeBanners.isEmpty()) {
            return;
        }

        int sort = 0;
        for (Banner banner : activeBanners) {
            if (!banner.getSort().equals(sort)) {
                Banner updateBanner = new Banner();
                updateBanner.setId(banner.getId());
                updateBanner.setSort(sort);
                updateBanner.setUpdateTime(LocalDateTime.now());
                bannerMapper.updateByPrimaryKeySelective(updateBanner);
            }
            sort++;
        }

        log.info("==> 轮播图重新排序完成, 总数: {}", sort);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> updateBannerSort(List<BannerSortRequest> sortRequests) {
        if (sortRequests == null || sortRequests.isEmpty()) {
            log.warn("==> 更新轮播图排序失败，排序请求为空");
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "排序请求不能为空");
        }

        log.info("==> 更新轮播图排序请求, 数量: {}", sortRequests.size());

        for (BannerSortRequest bannerSortRequest : sortRequests) {
            if (bannerSortRequest.getBannerId() == null) {
                throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "轮播图ID不能为空");
            }
            if (bannerSortRequest.getSort() == null) {
                throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "排序值不能为空");
            }

            Banner banner = bannerMapper.selectByPrimaryKey(bannerSortRequest.getBannerId());
            if (banner == null || banner.getIsDelete() == 1) {
                throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "轮播图不存在, bannerId: " + bannerSortRequest.getBannerId());
            }

            Banner updateBanner = new Banner();
            updateBanner.setId(bannerSortRequest.getBannerId());
            updateBanner.setSort(bannerSortRequest.getSort());
            updateBanner.setUpdateTime(LocalDateTime.now());
            bannerMapper.updateByPrimaryKeySelective(updateBanner);
        }

        log.info("==> 轮播图排序更新成功, 数量: {}", sortRequests.size());
        return Response.success();
    }
}
