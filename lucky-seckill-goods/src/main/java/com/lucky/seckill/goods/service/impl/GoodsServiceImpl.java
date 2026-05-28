package com.lucky.seckill.goods.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.manager.upload.FilePictureUpload;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.model.entity.Notify;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.mapper.GoodsDetailMapper;
import com.lucky.seckill.goods.mapper.GoodsImgMapper;
import com.lucky.seckill.goods.mapper.GoodsMapper;
import com.lucky.seckill.goods.mapper.GoodsSkuMapper;
import com.lucky.seckill.goods.model.dto.AddGoodsRequest;
import com.lucky.seckill.goods.model.dto.GoodsAuditRequest;
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
import com.lucky.seckill.notify.feign.NotifyFeignClient;
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

    @Resource
    private NotifyFeignClient notifyFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> addGoods(AddGoodsRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

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
                .userId(userId)
                .shopId(request.getShopId())
                .status(0)
                .collectNum(0)
                .isDelete(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .auditStatus(0)
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
                Goods.builder()
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

    /**
     * 获取规格图片
     * @param goodsId
     * @return
     */
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

    /**
     * 商品分页查询
     * @param goodsQueryRequest
     * @return
     */
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
        // 查询该商品所有规格的已售数量之和
        List<GoodsSku> skuList = goodsSkuMapper.selectByGoodsId(goods.getId());
        int totalSalesNum = skuList != null ? skuList.stream()
                .filter(sku -> sku.getIsDelete() == 0)
                .mapToInt(GoodsSku::getSalesNum)
                .sum() : 0;

        return GoodsListVO.builder()
                .id(goods.getId())
                .goodsName(goods.getGoodsName())
                .goodsImg(goods.getGoodsImg())
                .goodsPrice(goods.getGoodsPrice())
                .status(goods.getStatus())
                .statusDesc(getStatusDesc(goods.getStatus()))
                .salesNum(totalSalesNum)
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

    // 审核状态描述
    private String getAuditStatusDesc(Integer auditStatus) {
        if (auditStatus == null) return "未知";
        return switch (auditStatus) {
            case 0 -> "待审核";
            case 1 -> "审核通过";
            case 2 -> "审核拒绝";
            default -> "未知";
        };
    }

    /**
     * 上架申请
     * @param goodsId 商品ID
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> applyOnShelf(Long goodsId) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        if (goodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品ID不能为空");
        }
        // 根据商品ID查询商品信息
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null || goods.getIsDelete() == 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品不存在");
        }
        // 校验身份
        Long goodUserId=goods.getUserId();
        if(!userId.equals(goodUserId)){
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED,"该商品发布用户跟当前登录用户不同");
        }

        // 设置审核状态为待审核
        int rows = goodsMapper.updateAuditStatus(goodsId, 1, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "商品上架申请提交失败");
        }
        // 发送通知给管理员
        Notify notify = Notify.builder()
                .id(IdUtil.getSnowflakeNextId())
                .sendUserId(userId)
                .receiveUserId(5L) // 管理员ID
                .title("商品上架申请")
                .content(String.format("商品【%s】提交了上架申请，请到商品审核界面查看详情并处理", goods.getGoodsName()))
                .notifyType(2) // 商品上架申请类型
                .businessId(goodsId)
                .subType(1) // 1=商品
                .isRead(0)
                .isDelete(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        try {
            Response<Boolean> notifyResponse = notifyFeignClient.saveNotify(notify);
            if (notifyResponse == null || !notifyResponse.isSuccess()) {
                log.error("==> 商品上架申请通知发送失败, goodsId: {}", goodsId);
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "商品上架申请通知发送失败");
            }
        } catch (Exception e) {
            log.error("==> 调用消息服务发送商品上架申请通知失败, goodsId: {}, error: {}", goodsId, e.getMessage(), e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "商品上架申请通知发送失败");
        }

        clearGoodsPageCache();

        log.info("==> 商品上架申请提交成功, goodsId: {}", goodsId);
        return Response.success();
    }

    /**
     * 审核上架
     * @param request 审核请求
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> auditGoods(GoodsAuditRequest request) {
        log.info("==> 管理员审核商品上架申请, goodsId: {}, auditStatus: {}, auditRemark: {}",
                request.getGoodsId(), request.getAuditStatus(), request.getAuditRemark());

        Goods goods = goodsMapper.selectByPrimaryKey(request.getGoodsId());
        if (goods == null || goods.getIsDelete() == 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品不存在");
        }

        Integer auditStatus = goods.getAuditStatus();
        if (auditStatus != null && (auditStatus == 2 || auditStatus == 3)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "该商品已审核，请勿重复审核");
        }

        // 获取当前管理员ID
        Long adminId = StpUtil.getLoginIdAsLong();

        // 更新审核状态
        int rows = goodsMapper.updateAuditStatus(request.getGoodsId(), request.getAuditStatus(), LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "商品审核状态更新失败");
        }

        log.info("==> 商品审核状态更新成功, goodsId: {}, auditStatus: {}",
                request.getGoodsId(), request.getAuditStatus());

        if (request.getAuditStatus().equals(GoodsAuditRequest.AUDIT_PASS)) {
            // 审核通过，执行实际上架操作
            int shelfRows = goodsMapper.updateStatus(request.getGoodsId(), 1, LocalDateTime.now());
            if (shelfRows == 0) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "商品上架失败");
            }

            log.info("==> 商品上架成功, goodsId: {}", request.getGoodsId());

            // 发送审核通过通知给商家
            String title = "商品上架申请已通过";
            String content = String.format("恭喜！您的商品【%s】上架申请已通过审核！", goods.getGoodsName());

            Notify auditNotify = Notify.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .sendUserId(adminId)
                    .receiveUserId(goods.getUserId())
                    .title(title)
                    .content(content)
                    .notifyType(3) // 审核通知
                    .businessId(request.getGoodsId())
                    .subType(2) // 2=商品
                    .isRead(0)
                    .isDelete(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            try {
                Response<Boolean> auditNotifyResponse = notifyFeignClient.saveNotify(auditNotify);
                if (auditNotifyResponse == null || !auditNotifyResponse.isSuccess()) {
                    log.error("==> 审核通过通知发送失败, goodsId: {}", request.getGoodsId());
                    throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "审核通过通知发送失败");
                }
            } catch (Exception e) {
                log.error("==> 调用消息服务发送审核通过通知失败, goodsId: {}, error: {}",
                        request.getGoodsId(), e.getMessage(), e);
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "审核通过通知发送失败");
            }
            log.info("==> 审核通过通知已发送, notifyId: {}", auditNotify.getId());

        } else {
            // 审核拒绝
            String title = "商品上架申请被拒绝";
            String content = String.format("抱歉，您的商品【%s】上架申请未通过：%s",
                    goods.getGoodsName(), request.getAuditRemark());

            Notify rejectNotify = Notify.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .sendUserId(adminId)
                    .receiveUserId(goods.getUserId())
                    .title(title)
                    .content(content)
                    .notifyType(2) // 审核通知
                    .businessId(request.getGoodsId())
                    .subType(2) // 2=商品
                    .isRead(0)
                    .isDelete(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            try {
                Response<Boolean> rejectNotifyResponse = notifyFeignClient.saveNotify(rejectNotify);
                if (rejectNotifyResponse == null || !rejectNotifyResponse.isSuccess()) {
                    log.error("==> 审核拒绝通知发送失败, goodsId: {}", request.getGoodsId());
                    throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "审核拒绝通知发送失败");
                }
            } catch (Exception e) {
                log.error("==> 调用消息服务发送审核拒绝通知失败, goodsId: {}, error: {}",
                        request.getGoodsId(), e.getMessage(), e);
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "审核拒绝通知发送失败");
            }
            log.info("==> 审核拒绝通知已发送, notifyId: {}", rejectNotify.getId());
        }

        clearGoodsPageCache();

        log.info("==> 商品审核完成, goodsId: {}, auditStatus: {}",
                request.getGoodsId(), request.getAuditStatus());

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
    public Response<Goods> getGoodsById(Long goodsId) {
        log.info("==> getGoodsById 开始查询，goodsId: {}", goodsId);
        if (goodsId == null) {
            log.warn("==> 商品ID为空");
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品ID不能为空");
        }
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        log.info("==> 数据库查询结果: goods = {}", goods);
        if (goods != null) {
            log.info("==> 商品详情: id={}, goodsName={}, isDelete={}, status={}", 
                    goods.getId(), goods.getGoodsName(), goods.getIsDelete(), goods.getStatus());
        }
        if (goods == null) {
            log.warn("==> 商品不存在，数据库查询返回null，goodsId: {}", goodsId);
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品不存在");
        }
        log.info("==> 查询商品信息成功, goodsId: {}, goodsName: {}", goodsId, goods.getGoodsName());
        return Response.success(goods);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> lockStock(Long skuId, Integer buyGoodsNum) {
        if (skuId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU ID不能为空");
        }
        if (buyGoodsNum == null || buyGoodsNum <= 0) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "预占数量必须大于0");
        }

        // 查询SKU确认存在
        GoodsSku sku = goodsSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU不存在");
        }

        // 预占库存（使用行锁保证原子性）
        int rows = goodsSkuMapper.lockStock(skuId, buyGoodsNum);
        if (rows == 0) {
            log.warn("==> 预占库存失败，库存不足，skuId: {}, 请求数量: {}, 当前库存: {}",
                    skuId, buyGoodsNum, sku.getStockNum());
            return Response.success(false);
        }

        log.info("==> 预占库存成功，skuId: {}, 预占数量: {}", skuId, buyGoodsNum);
        return Response.success(true);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> releaseStock(Long skuId, Integer stockNum) {
        if (skuId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU ID不能为空");
        }
        if (stockNum == null || stockNum <= 0) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "释放数量必须大于0");
        }

        // 查询SKU确认存在
        GoodsSku sku = goodsSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU不存在");
        }

        // 释放预占的库存
        int rows = goodsSkuMapper.releaseStock(skuId, stockNum);
        if (rows == 0) {
            log.error("==> 释放库存失败，skuId: {}", skuId);
            return Response.success(false);
        }

        log.info("==> 释放库存成功，skuId: {}, 释放数量: {}", skuId, stockNum);
        return Response.success(true);
    }

}