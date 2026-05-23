        package com.lucky.seckill.common.service.impl;

import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.manager.upload.FilePictureUpload;
import com.lucky.seckill.common.mapper.BannerMapper;
import com.lucky.seckill.common.model.entity.Banner;
import com.lucky.seckill.common.model.file.UploadPictureResult;
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
    public Response<List<String>> saveBannerImgUrls(List<String> imgUrls) {
        if (imgUrls == null || imgUrls.isEmpty()) {
            log.warn("==> 保存背景图URL数组失败，URL数组为空");
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "请提供图片URL");
        }

        List<String> validUrls = new ArrayList<>();
        List<Banner> banners = new ArrayList<>();
        int sort = 0;
        for (String url : imgUrls) {
            if (url != null && !url.trim().isEmpty()) {
                String trimmed = url.trim();
                validUrls.add(trimmed);
                Banner banner = Banner.builder()
                        .bannerUrl(trimmed)
                        .sort(sort++)
                        .status(1)
                        .isDelete(0)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build();
                banners.add(banner);
                bannerMapper.insertSelective(banner);
            }
        }

        if (validUrls.isEmpty()) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "没有有效的图片URL");
        }

        log.info("==> 背景图URL数组保存成功, 总数: {}", validUrls.size());
        return Response.success(validUrls);
    }
}
