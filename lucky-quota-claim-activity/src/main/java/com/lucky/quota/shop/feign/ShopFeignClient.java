package com.lucky.quota.shop.feign;

import com.lucky.quota.common.model.file.UploadPictureResult;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.shop.model.vo.ShopListVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "lucky-quota-claim-goods", path = "/shop")
public interface ShopFeignClient {

    @GetMapping("/{shopId}")
    Response<ShopListVO> getShopById(@PathVariable("shopId") Long shopId);

    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response<UploadPictureResult> uploadShopAvatar(@RequestPart("file") MultipartFile file);
}