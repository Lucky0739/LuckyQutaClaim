package com.lucky.seckill.common.service;

import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BannerService {
    Response<UploadPictureResult> uploadBannerImg(MultipartFile file);

    Response<List<String>> saveBannerImgUrls(List<String> imgUrls);
}