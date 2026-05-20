package com.lucky.seckill.goods.service.impl;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.manager.upload.FilePictureUpload;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.mapper.GoodsDetailMapper;
import com.lucky.seckill.goods.mapper.GoodsImgMapper;
import com.lucky.seckill.goods.mapper.GoodsMapper;
import com.lucky.seckill.goods.mapper.GoodsSkuMapper;
import com.lucky.seckill.goods.model.dto.AddGoodsRequest;
import com.lucky.seckill.goods.model.dto.GoodsSkuRequest;
import com.lucky.seckill.goods.model.entity.Goods;
import com.lucky.seckill.goods.model.entity.GoodsDetail;
import com.lucky.seckill.goods.model.entity.GoodsImg;
import com.lucky.seckill.goods.model.entity.GoodsSku;
import com.lucky.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class GoodsServiceImpl implements GoodsService {

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private GoodsDetailMapper goodsDetailMapper;

    @Resource
    private GoodsImgMapper goodsImgMapper;

    @Resource
    private GoodsSkuMapper goodsSkuMapper;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> addGoods(AddGoodsRequest request) {
        long goodsId = IdUtil.getSnowflakeNextId();

        String goodsImg = null;
        List<GoodsSkuRequest> skuList = request.getSkuList();
        if (skuList != null && !skuList.isEmpty()) {
            GoodsSkuRequest firstSku = skuList.get(0);
            if (firstSku.getSkuImg() != null && !firstSku.getSkuImg().isEmpty()) {
                goodsImg = firstSku.getSkuImg();
            }
        }

        Goods goods = Goods.builder()
                .id(goodsId)
                .goodsName(request.getGoodsName())
                .goodsImg(goodsImg)
                .goodsPrice(request.getGoodsPrice())
                .status(1)
                .isDelete(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        goodsMapper.insertSelective(goods);

        if (request.getDetailContent() != null && !request.getDetailContent().isEmpty()) {
            GoodsDetail detail = GoodsDetail.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .goodsId(goodsId)
                    .detailContent(request.getDetailContent())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            goodsDetailMapper.insertSelective(detail);
        }

        List<String> goodsImgList = request.getGoodsImgList();
        if (goodsImgList != null && !goodsImgList.isEmpty()) {
            for (int i = 0; i < goodsImgList.size(); i++) {
                String imgUrl = goodsImgList.get(i);
                if (imgUrl != null && !imgUrl.isEmpty()) {
                    GoodsImg goodsImgEntity = GoodsImg.builder()
                            .id(IdUtil.getSnowflakeNextId())
                            .goodsId(goodsId)
                            .imgUrl(imgUrl)
                            .sort(i)
                            .createTime(LocalDateTime.now())
                            .build();
                    goodsImgMapper.insertSelective(goodsImgEntity);
                }
            }

            if (goods.getGoodsImg() == null || goods.getGoodsImg().isEmpty()) {
                Goods updateGoods = Goods.builder()
                        .id(goodsId)
                        .goodsImg(goodsImgList.get(0))
                        .updateTime(LocalDateTime.now())
                        .build();
                goodsMapper.updateByPrimaryKeySelective(updateGoods);
            }
        }

        if (skuList != null && !skuList.isEmpty()) {
            for (GoodsSkuRequest skuDTO : skuList) {
                GoodsSku sku = GoodsSku.builder()
                        .id(IdUtil.getSnowflakeNextId())
                        .goodsId(goodsId)
                        .skuSpecName(skuDTO.getSkuSpecName() != null ? skuDTO.getSkuSpecName() : "")
                        .skuImg(skuDTO.getSkuImg() != null ? skuDTO.getSkuImg() : "")
                        .marketPrice(skuDTO.getMarketPrice() != null ? skuDTO.getMarketPrice() : request.getGoodsPrice())
                        .salePrice(skuDTO.getSalePrice() != null ? skuDTO.getSalePrice() : request.getGoodsPrice())
                        .stockNum(skuDTO.getStockNum() != null ? skuDTO.getStockNum() : 0)
                        .salesNum(0)
                        .status(1)
                        .isDelete(0)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build();
                goodsSkuMapper.insertSelective(sku);
            }
        }

        log.info("==> 新增商品成功, goodsId: {}, skuCount: {}", goodsId, skuList != null ? skuList.size() : 0);
        return Response.success(goodsId);
    }

    @Override
    public Response<UploadPictureResult> uploadGoodsImg(MultipartFile file) {
        UploadPictureResult uploadResult = filePictureUpload.uploadPicture(file, "goods", true);
        log.info("==> 商品图片上传成功, imgUrl: {}", uploadResult.getImageUrl());
        return Response.success(uploadResult);
    }
}