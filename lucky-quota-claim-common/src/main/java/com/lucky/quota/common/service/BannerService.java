
package com.lucky.quota.common.service;

import com.lucky.quota.common.model.file.UploadPictureResult;
import com.lucky.quota.common.model.request.BannerRequest;
import com.lucky.quota.common.model.request.BannerSortRequest;
import com.lucky.quota.common.utils.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BannerService {
    Response<UploadPictureResult> uploadBannerImg(MultipartFile file);

    Response<List<String>> saveBannerImgUrls(List<BannerRequest> bannerRequests);

    Response<List<String>> getBannerList();

    Response<?> deleteBanner(Long bannerId);

    Response<?> updateBannerSort(List<BannerSortRequest> sortRequests);
}
