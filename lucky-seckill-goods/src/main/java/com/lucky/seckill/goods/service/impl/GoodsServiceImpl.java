package com.lucky.seckill.goods.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.manager.upload.FilePictureUpload;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.mapper.GoodsDetailMapper;
import com.lucky.seckill.goods.mapper.GoodsImgMapper;
import com.lucky.seckill.goods.mapper.GoodsMapper;
import com.lucky.seckill.goods.mapper.GoodsSkuMapper;
import com.lucky.seckill.goods.model.dto.AddGoodsRequest;
import com.lucky.seckill.goods.model.dto.GoodsQueryRequest;
import com.lucky.seckill.goods.model.dto.GoodsSkuRequest;
import com.lucky.seckill.goods.model.entity.Goods;
import com.lucky.seckill.goods.model.entity.GoodsDetail;
import com.lucky.seckill.goods.model.entity.GoodsImg;
import com.lucky.seckill.goods.model.entity.GoodsSku;
import com.lucky.seckill.goods.model.vo.GoodsDetailVO;
import com.lucky.seckill.goods.model.vo.GoodsListVO;
import com.lucky.seckill.goods.model.vo.SkuImgVO;
import com.lucky.seckill.goods.model.vo.SkuInfoVO;
import com.lucky.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
                .categoryId(request.getCategoryId())
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
            }
            goodsMapper.updateByPrimaryKeySelective(goods);
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

    @Override
    public Response<GoodsDetailVO> getGoodsDetail(Long goodsId) {
        if (goodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品ID不能为空");
        }

        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null || goods.getIsDelete() == 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品不存在");
        }

        List<GoodsImg> goodsImgList = goodsImgMapper.selectByGoodsId(goodsId);
        List<String> imgUrlList = goodsImgList != null
                ? goodsImgList.stream().map(GoodsImg::getImgUrl).collect(Collectors.toList())
                : Collections.emptyList();

        GoodsDetail goodsDetail = goodsDetailMapper.selectByGoodsId(goodsId);
        String detailContent = goodsDetail != null ? goodsDetail.getDetailContent() : "";

        List<GoodsSku> skuList = goodsSkuMapper.selectByGoodsId(goodsId);
        List<SkuInfoVO> skuInfoList = skuList != null
                ? skuList.stream()
                .filter(sku -> sku.getIsDelete() == 0)
                .map(sku -> SkuInfoVO.builder()
                        .skuId(sku.getId())
                        .skuSpecName(sku.getSkuSpecName())
                        .skuImg(sku.getSkuImg())
                        .marketPrice(sku.getMarketPrice())
                        .salePrice(sku.getSalePrice())
                        .stockNum(sku.getStockNum())
                        .salesNum(sku.getSalesNum())
                        .build())
                .collect(Collectors.toList())
                : Collections.emptyList();

        GoodsDetailVO vo = GoodsDetailVO.builder()
                .goodsId(goods.getId())
                .goodsName(goods.getGoodsName())
                .goodsImg(goods.getGoodsImg())
                .goodsPrice(goods.getGoodsPrice())
                .goodsImgList(imgUrlList)
                .detailContent(detailContent)
                .skuList(skuInfoList)
                .build();

        log.info("==> 查询商品详情成功, goodsId: {}", goodsId);
        return Response.success(vo);
    }

    @Override
    public Response<List<SkuImgVO>> getSkuImages(Long goodsId) {
        if (goodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品ID不能为空");
        }

        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null || goods.getIsDelete() == 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品不存在");
        }

        List<GoodsSku> skuList = goodsSkuMapper.selectByGoodsId(goodsId);
        if (skuList == null || skuList.isEmpty()) {
            return Response.success(Collections.emptyList());
        }

        List<SkuImgVO> skuImgList = skuList.stream()
                .filter(sku -> sku.getIsDelete() == 0)
                .map(sku -> SkuImgVO.builder()
                        .skuId(sku.getId())
                        .skuSpecName(sku.getSkuSpecName())
                        .skuImg(sku.getSkuImg())
                        .build())
                .collect(Collectors.toList());

        log.info("==> 查询商品规格图片成功, goodsId: {}, count: {}", goodsId, skuImgList.size());
        return Response.success(skuImgList);
    }

    @Override
    public Response<PageResult<GoodsListVO>> getGoodsPage(GoodsQueryRequest goodsQueryRequest) {
        // 1. 分页参数
        int pageNum = goodsQueryRequest.getPageNum() == null ? 1 : goodsQueryRequest.getPageNum();
        int pageSize = goodsQueryRequest.getPageSize() == null ? 10 : goodsQueryRequest.getPageSize();

        // 2. 开启分页
        try (Page<Goods> page = PageHelper.startPage(pageNum, pageSize)) {

            // 3. 时间转换
            Date startTime = Optional.ofNullable(goodsQueryRequest.getStartTime())
                    .map(ld -> Date.from(ld.atZone(ZoneId.systemDefault()).toInstant())).orElse(null);
            Date endTime = Optional.ofNullable(goodsQueryRequest.getEndTime())
                    .map(ld -> Date.from(ld.atZone(ZoneId.systemDefault()).toInstant())).orElse(null);

            // 4. 查询DB
            List<Goods> goodsList = goodsMapper.selectGoodsPage(goodsQueryRequest);

            // 5. 转VO（必须在分页内转，才能拿到total）
            List<GoodsListVO> voList = goodsList.stream()
                    .map(this::convertGoodsToVO)
                    .collect(Collectors.toList());

            // 6. 拼接分页结果
            PageResult<GoodsListVO> pageResult = PageResult.<GoodsListVO>builder()
                    .records(voList)
                    .total(page.getTotal())
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                    .build();

            // 7. 缓存VO（key必须带条件，否则覆盖）
            String cacheKey = "goods:page:" + DigestUtils.md5Hex(JSON.toJSONString(goodsQueryRequest));
            stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(pageResult), 10, TimeUnit.MINUTES);

            log.info("==> 商品分页查询成功，总数：{}，当前页：{}", page.getTotal(), pageNum);
            return Response.success(pageResult);
        }
    }

    // VO转换抽成方法（优雅、可复用）
    private GoodsListVO convertGoodsToVO(Goods goods) {
        return GoodsListVO.builder()
                .id(goods.getId())
                .goodsName(goods.getGoodsName())
                .goodsImg(goods.getGoodsImg())
                .goodsPrice(goods.getGoodsPrice())
                .status(goods.getStatus())
                .statusDesc(getStatusDesc(goods.getStatus()))
                .createTime(goods.getCreateTime())
                .updateTime(goods.getUpdateTime())
                .build();
    }

    // 状态描述
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "下架";
            case 1 -> "上架";
            default -> "未知";
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> onShelf(Long goodsId) {
        if (goodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品ID不能为空");
        }

        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null || goods.getIsDelete() == 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品不存在");
        }

        if (goods.getStatus() == 1) {
            log.warn("==> 商品已处于上架状态, goodsId: {}", goodsId);
            return Response.success();
        }

        int rows = goodsMapper.updateStatus(goodsId, 1, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "商品上架失败");
        }

        clearGoodsPageCache();

        log.info("==> 商品上架成功, goodsId: {}", goodsId);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> offShelf(Long goodsId) {
        if (goodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品ID不能为空");
        }

        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null || goods.getIsDelete() == 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品不存在");
        }

        if (goods.getStatus() == 0) {
            log.warn("==> 商品已处于下架状态, goodsId: {}", goodsId);
            return Response.success();
        }

        int rows = goodsMapper.updateStatus(goodsId, 0, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "商品下架失败");
        }

        clearGoodsPageCache();

        log.info("==> 商品下架成功, goodsId: {}", goodsId);
        return Response.success();
    }

    private void clearGoodsPageCache() {
        java.util.Set<String> keys = stringRedisTemplate.keys("goods:page:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            log.info("==> 清除商品分页缓存，共{}个key", keys.size());
        }
    }

    @Override
    public Response<GoodsSku> getSkuById(Long skuId) {
        if (skuId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU ID不能为空");
        }

        GoodsSku sku = goodsSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU不存在");
        }

        log.info("==> 查询SKU信息成功, skuId: {}", skuId);
        return Response.success(sku);
    }
}