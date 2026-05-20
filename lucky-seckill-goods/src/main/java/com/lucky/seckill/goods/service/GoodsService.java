package com.lucky.seckill.goods.service;

import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.model.dto.AddGoodsRequest;
import com.lucky.seckill.goods.model.dto.GoodsSkuRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GoodsService {
    /**
     * 新增商品
     * @param request
     * @return
     */
    Response<Long> addGoods(AddGoodsRequest request);

    /**
     * 上传商品图片
     * @param file
     * @return
     */
    Response<UploadPictureResult> uploadGoodsImg(MultipartFile file);
}