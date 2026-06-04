package com.lucky.seckill.shop.feign;

import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.shop.model.vo.ShopListVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "lucky-seckill-shop", path = "/shop")
public interface ShopFeignClient {

    @GetMapping("/{shopId}")
    Response<ShopListVO> getShopById(@PathVariable("shopId") Long shopId);

    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response<UploadPictureResult> uploadShopAvatar(@RequestPart("file") MultipartFile file);
}