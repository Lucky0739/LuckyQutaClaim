package com.lucky.seckill.goods.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.manager.upload.FilePictureUpload;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.goods.mapper.GoodsCategoryMapper;
import com.lucky.seckill.goods.mapper.GoodsDetailMapper;
import com.lucky.seckill.goods.mapper.GoodsImgMapper;
import com.lucky.seckill.goods.mapper.GoodsMapper;
import com.lucky.seckill.goods.mapper.GoodsSkuMapper;
import com.lucky.seckill.goods.enums.GoodsRankSortEnum;
import com.lucky.seckill.goods.model.dto.AddGoodsRequest;
import com.lucky.seckill.goods.model.dto.GoodsAuditRequest;
import com.lucky.seckill.goods.model.dto.GoodsHomeQueryRequest;
import com.lucky.seckill.goods.model.dto.GoodsQueryRequest;
import com.lucky.seckill.goods.model.dto.GoodsSearchRequest;
import com.lucky.seckill.goods.model.dto.GoodsSkuRequest;
import com.lucky.seckill.goods.model.dto.UpdateGoodsRequest;
import com.lucky.seckill.goods.model.dto.UpdateGoodsSkuRequest;
import com.lucky.seckill.goods.model.vo.MerchantGoodsDetailVO;
import com.lucky.seckill.goods.model.vo.MerchantGoodsListVO;
import com.lucky.seckill.goods.elasticsearch.service.GoodsElasticsearchService;
import com.lucky.seckill.goods.service.GoodsCacheService;
import com.lucky.seckill.goods.service.GoodsRankRedisService;
import com.lucky.seckill.goods.model.entity.Goods;
import com.lucky.seckill.goods.model.entity.GoodsCategory;
import com.lucky.seckill.goods.model.entity.GoodsDetail;
import com.lucky.seckill.goods.model.entity.GoodsImg;
import com.lucky.seckill.goods.model.entity.GoodsSku;
import com.lucky.seckill.goods.model.vo.GoodsDetailVO;
import com.lucky.seckill.goods.model.vo.GoodsListVO;
import com.lucky.seckill.goods.model.vo.SkuInfoVO;
import com.lucky.seckill.goods.service.GoodsService;
import com.lucky.seckill.notify.feign.NotifyFeignClient;
import com.lucky.seckill.notify.model.entity.Notify;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoodsServiceImpl implements GoodsService {

    private static final long PARENT_CATEGORY_ROOT_ID = 0L;

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private GoodsCategoryMapper goodsCategoryMapper;

    @Resource
    private GoodsDetailMapper goodsDetailMapper;

    @Resource
    private GoodsImgMapper goodsImgMapper;

    @Resource
    private GoodsSkuMapper goodsSkuMapper;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private NotifyFeignClient notifyFeignClient;

    @Resource
    private GoodsRankRedisService goodsRankRedisService;

    @Resource
    private GoodsCacheService goodsCacheService;

    @Resource
    private GoodsElasticsearchService goodsElasticsearchService;

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

    /**
     * 查看商品详情
     * @param goodsId
     * @return
     */
    @Override
    public Response<GoodsDetailVO> getGoodsDetail(Long goodsId) {
        if (goodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品ID不能为空");
        }

        GoodsDetailVO vo = goodsCacheService.getGoodsDetail(goodsId, () -> loadGoodsDetailFromDb(goodsId));
        log.info("==> 查询商品详情成功, goodsId: {}", goodsId);
        return Response.success(vo);
    }

    private GoodsDetailVO loadGoodsDetailFromDb(Long goodsId) {
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

        return GoodsDetailVO.builder()
                .goodsId(goods.getId())
                .goodsName(goods.getGoodsName())
                .goodsImg(goods.getGoodsImg())
                .goodsPrice(goods.getGoodsPrice())
                .goodsImgList(imgUrlList)
                .detailContent(detailContent)
                .skuList(skuInfoList)
                .build();
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

        // 2. 解析父/子分类查询条件
        prepareCategoryQuery(goodsQueryRequest);
        if (goodsQueryRequest.getCategoryIds() != null && goodsQueryRequest.getCategoryIds().isEmpty()) {
            log.info("==> 父分类下无子分类，返回空列表, parentCategoryId: {}",
                    goodsQueryRequest.getParentCategoryId());
            return Response.success(buildEmptyPageResult(pageNum, pageSize));
        }

        // 3. 逻辑过期 + 互斥锁读缓存，未命中则查库并回填
        PageResult<GoodsListVO> pageResult = goodsCacheService.getGoodsPage(
                goodsQueryRequest,
                () -> queryGoodsPageFromDb(goodsQueryRequest, pageNum, pageSize));

        log.info("==> 商品分页查询成功，总数：{}，当前页：{}", pageResult.getTotal(), pageNum);
        return Response.success(pageResult);
    }

    @Override
    public Response<PageResult<GoodsListVO>> getPendingAuditGoodsPage(GoodsQueryRequest request) {
        request.setAuditStatus(1);
        request.setAuditStatusIn(null);
        return Response.success(queryAdminGoodsPageFromDb(request));
    }

    @Override
    public Response<PageResult<GoodsListVO>> getAuditedGoodsPage(GoodsQueryRequest request) {
        if (request.getAuditStatus() == null
                && (request.getAuditStatusIn() == null || request.getAuditStatusIn().isEmpty())) {
            request.setAuditStatusIn(Arrays.asList(
                    GoodsAuditRequest.AUDIT_PASS,
                    GoodsAuditRequest.AUDIT_REJECT));
        }
        return Response.success(queryAdminGoodsPageFromDb(request));
    }

    /**
     * 管理端商品列表：绕过 Redis 分页缓存，保证审核场景与数据库实时一致
     */
    private PageResult<GoodsListVO> queryAdminGoodsPageFromDb(GoodsQueryRequest request) {
        int pageNum = request.getPageNum() == null ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();

        prepareCategoryQuery(request);
        if (request.getCategoryIds() != null && request.getCategoryIds().isEmpty()) {
            return buildEmptyPageResult(pageNum, pageSize);
        }

        PageResult<GoodsListVO> pageResult = queryGoodsPageFromDb(request, pageNum, pageSize);
        log.info("==> 管理端商品列表查库完成, auditStatus: {}, total: {}",
                request.getAuditStatus(), pageResult.getTotal());
        return pageResult;
    }

    private PageResult<GoodsListVO> queryGoodsPageFromDb(GoodsQueryRequest goodsQueryRequest,
                                                           int pageNum,
                                                           int pageSize) {
        try (Page<Goods> page = PageHelper.startPage(pageNum, pageSize)) {
            List<Goods> goodsList = goodsMapper.selectGoodsPage(goodsQueryRequest);
            List<GoodsListVO> voList = goodsList.stream()
                    .map(this::convertGoodsToVO)
                    .collect(Collectors.toList());

            return PageResult.<GoodsListVO>builder()
                    .records(voList)
                    .total(page.getTotal())
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                    .build();
        }
    }

    @Override
    public Response<PageResult<GoodsListVO>> getHomeGoodsList(GoodsHomeQueryRequest request) {
        GoodsRankSortEnum sortType = GoodsRankSortEnum.fromCode(
                request == null ? null : request.getSortType());
        if (sortType == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID,
                    "sortType 支持 weight（综合权重，默认）、collect（收藏量）、sales（已售数量），hot 等同 weight");
        }

        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;

        long total = goodsRankRedisService.countRank(sortType);
        if (total == 0) {
            goodsRankRedisService.rebuildRankFromDb();
            total = goodsRankRedisService.countRank(sortType);
        }

        List<Long> goodsIds = goodsRankRedisService.listGoodsIdsByRank(sortType, offset, pageSize);
        if (goodsIds.isEmpty()) {
            return Response.success(buildEmptyPageResult(pageNum, pageSize));
        }

        List<Goods> goodsList = goodsMapper.selectOnShelfByIds(goodsIds);
        Map<Long, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<GoodsListVO> voList = goodsIds.stream()
                .map(goodsMap::get)
                .filter(Objects::nonNull)
                .map(this::convertGoodsToVO)
                .collect(Collectors.toList());

        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        PageResult<GoodsListVO> pageResult = PageResult.<GoodsListVO>builder()
                .records(voList)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();

        log.info("==> 首页商品列表查询成功, sortType: {}, total: {}, pageNum: {}", sortType.getCode(), total, pageNum);
        return Response.success(pageResult);
    }

    @Override
    public Response<PageResult<GoodsListVO>> searchGoods(GoodsSearchRequest request) {
        if (request == null || !StringUtils.hasText(request.getKeyword())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "搜索关键词不能为空");
        }

        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
        request.setKeyword(request.getKeyword().trim());

        prepareSearchCategoryQuery(request);
        if (request.getCategoryIds() != null && request.getCategoryIds().isEmpty()) {
            return Response.success(buildEmptyPageResult(pageNum, pageSize));
        }

        if (goodsElasticsearchService.isEnabled()) {
            try {
                PageResult<GoodsListVO> esResult = goodsElasticsearchService.search(
                        request.getKeyword(), request.getCategoryIds(), request);
                log.info("==> 商品 ES 搜索成功, keyword: {}, total: {}", request.getKeyword(), esResult.getTotal());
                return Response.success(esResult);
            } catch (Exception e) {
                log.warn("==> 商品 ES 搜索失败，降级 MySQL, keyword: {}, error: {}",
                        request.getKeyword(), e.getMessage());
            }
        }

        GoodsQueryRequest dbQuery = GoodsQueryRequest.builder()
                .goodsName(request.getKeyword())
                .categoryId(request.getCategoryId())
                .parentCategoryId(request.getParentCategoryId())
                .categoryIds(request.getCategoryIds())
                .shopId(request.getShopId())
                .status(1)
                .auditStatus(GoodsAuditRequest.AUDIT_PASS)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        PageResult<GoodsListVO> dbResult = queryGoodsPageFromDb(dbQuery, pageNum, pageSize);
        log.info("==> 商品 MySQL 搜索完成, keyword: {}, total: {}", request.getKeyword(), dbResult.getTotal());
        return Response.success(dbResult);
    }

    private void prepareSearchCategoryQuery(GoodsSearchRequest request) {
        GoodsQueryRequest temp = GoodsQueryRequest.builder()
                .categoryId(request.getCategoryId())
                .parentCategoryId(request.getParentCategoryId())
                .categoryIds(request.getCategoryIds())
                .build();
        prepareCategoryQuery(temp);
        request.setCategoryId(temp.getCategoryId());
        request.setParentCategoryId(temp.getParentCategoryId());
        request.setCategoryIds(temp.getCategoryIds());
    }

    // VO转换抽成方法（优雅、可复用）
    private GoodsListVO convertGoodsToVO(Goods goods) {
        // 查询该商品所有规格的已售数量之和
        List<GoodsSku> skuList = goodsSkuMapper.selectByGoodsId(goods.getId());
        int totalSalesNum = skuList != null ? skuList.stream()
                .filter(sku -> sku.getIsDelete() == 0)
                .mapToInt(GoodsSku::getSalesNum)
                .sum() : 0;

        GoodsListVO vo = GoodsListVO.builder()
                .id(goods.getId())
                .goodsName(goods.getGoodsName())
                .goodsImg(goods.getGoodsImg())
                .goodsPrice(goods.getGoodsPrice())
                .salesNum(totalSalesNum)
                .categoryId(goods.getCategoryId())
                .status(goods.getStatus())
                .auditStatus(goods.getAuditStatus())
                .shopId(goods.getShopId())
                .createTime(goods.getCreateTime())
                .build();
        fillCategoryInfo(vo, goods.getCategoryId());
        return vo;
    }

    /**
     * 解析商品分类查询：支持子分类精确查询、父分类聚合查询
     */
    private void prepareCategoryQuery(GoodsQueryRequest request) {
        Long parentCategoryId = request.getParentCategoryId();
        Long categoryId = request.getCategoryId();

        if (categoryId != null && parentCategoryId != null) {
            validateChildUnderParent(categoryId, parentCategoryId);
            return;
        }

        if (categoryId != null) {
            GoodsCategory category = requireCategory(categoryId);
            if (isParentCategory(category)) {
                request.setParentCategoryId(categoryId);
                request.setCategoryId(null);
                request.setCategoryIds(goodsCategoryMapper.selectIdsByParentId(categoryId));
            }
            return;
        }

        if (parentCategoryId != null) {
            validateParentCategory(parentCategoryId);
            request.setCategoryIds(goodsCategoryMapper.selectIdsByParentId(parentCategoryId));
        }
    }

    private void validateParentCategory(Long parentCategoryId) {
        GoodsCategory parent = requireCategory(parentCategoryId);
        if (!isParentCategory(parent)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "parentCategoryId 必须为父分类");
        }
    }

    private void validateChildUnderParent(Long categoryId, Long parentCategoryId) {
        validateParentCategory(parentCategoryId);
        GoodsCategory child = requireCategory(categoryId);
        if (!parentCategoryId.equals(child.getParentId())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "子分类不属于该父分类");
        }
    }

    private GoodsCategory requireCategory(Long categoryId) {
        GoodsCategory category = goodsCategoryMapper.selectByPrimaryKey(categoryId);
        if (category == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "分类不存在");
        }
        return category;
    }

    private boolean isParentCategory(GoodsCategory category) {
        Long parentId = category.getParentId();
        return parentId == null || parentId == PARENT_CATEGORY_ROOT_ID;
    }

    private void fillCategoryInfo(GoodsListVO vo, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        GoodsCategory category = goodsCategoryMapper.selectByPrimaryKey(categoryId);
        if (category == null) {
            return;
        }
        vo.setCategoryName(category.getCategoryName());
        if (isParentCategory(category)) {
            vo.setParentCategoryId(category.getId());
            vo.setParentCategoryName(category.getCategoryName());
            return;
        }
        vo.setParentCategoryId(category.getParentId());
        GoodsCategory parent = goodsCategoryMapper.selectByPrimaryKey(category.getParentId());
        if (parent != null) {
            vo.setParentCategoryName(parent.getCategoryName());
        }
    }

    private PageResult<GoodsListVO> buildEmptyPageResult(int pageNum, int pageSize) {
        return PageResult.<GoodsListVO>builder()
                .records(Collections.emptyList())
                .total(0L)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(0)
                .build();
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
        assertGoodsOwner(goods);

        if (Integer.valueOf(1).equals(goods.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品已在待审核中，请勿重复申请");
        }
        if (GoodsAuditRequest.AUDIT_PASS.equals(goods.getAuditStatus())
                && Integer.valueOf(1).equals(goods.getStatus())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品已上架，无需重复申请");
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

        invalidateGoodsCache(goodsId);

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
            goodsRankRedisService.refreshGoodsRankIfOnShelf(request.getGoodsId());

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

        invalidateGoodsCache(request.getGoodsId());

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

        Goods goods = requireGoods(goodsId);
        assertGoodsOwner(goods);

        if (goods.getStatus() == 0) {
            log.warn("==> 商品已处于下架状态, goodsId: {}", goodsId);
            return Response.success();
        }

        int rows = goodsMapper.updateStatus(goodsId, 0, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "商品下架失败");
        }

        invalidateGoodsCache(goodsId);
        goodsRankRedisService.removeFromRank(goodsId);

        log.info("==> 商品下架成功, goodsId: {}", goodsId);
        return Response.success();
    }

    private void invalidateGoodsCache(Long goodsId) {
        goodsCacheService.clearPageCache();
        goodsCacheService.evictGoodsCache(goodsId);
        syncSearchIndex(goodsId);
    }

    private void syncSearchIndex(Long goodsId) {
        if (!goodsElasticsearchService.isEnabled()) {
            return;
        }
        try {
            goodsElasticsearchService.syncGoods(goodsId);
        } catch (Exception e) {
            log.warn("==> 商品 ES 索引同步失败, goodsId: {}, error: {}", goodsId, e.getMessage());
        }
    }

    @Override
    public Response<Goods> getGoodsById(Long goodsId) {
        if (goodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品ID不能为空");
        }
        Goods goods = goodsCacheService.getGoodsById(goodsId, () -> loadGoodsFromDb(goodsId));
        log.info("==> 查询商品信息成功, goodsId: {}, goodsName: {}", goodsId, goods.getGoodsName());
        return Response.success(goods);
    }

    private Goods loadGoodsFromDb(Long goodsId) {
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品不存在");
        }
        return goods;
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

        // 预占库存（校验库存是否充足，支付成功后再真正扣减）
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> deductStock(Long skuId, Integer stockNum) {
        if (skuId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU ID不能为空");
        }
        if (stockNum == null || stockNum <= 0) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "扣减数量必须大于0");
        }

        GoodsSku sku = goodsSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU不存在");
        }

        int rows = goodsSkuMapper.deductStock(skuId, stockNum);
        if (rows == 0) {
            log.error("==> 扣减库存失败，skuId: {}", skuId);
            return Response.success(false);
        }

        log.info("==> 扣减库存成功，skuId: {}, 扣减数量: {}, 已售数量+{}", skuId, stockNum, stockNum);
        goodsRankRedisService.refreshGoodsRankIfOnShelf(sku.getGoodsId());
        return Response.success(true);
    }

    @Override
    public Response<PageResult<MerchantGoodsListVO>> getMyGoodsPage(GoodsQueryRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        request.setUserId(userId);

        int pageNum = request.getPageNum() == null ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();

        if (request.getCategoryId() != null) {
            prepareCategoryQuery(request);
            if (request.getCategoryIds() != null && request.getCategoryIds().isEmpty()) {
                return Response.success(buildEmptyMerchantPageResult(pageNum, pageSize));
            }
        }

        try (Page<Goods> page = PageHelper.startPage(pageNum, pageSize)) {
            List<Goods> goodsList = goodsMapper.selectGoodsPage(request);
            List<MerchantGoodsListVO> voList = goodsList.stream()
                    .map(this::convertMerchantGoodsToVO)
                    .collect(Collectors.toList());

            PageResult<MerchantGoodsListVO> pageResult = PageResult.<MerchantGoodsListVO>builder()
                    .records(voList)
                    .total(page.getTotal())
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .totalPages((int) Math.ceil((double) page.getTotal() / pageSize))
                    .build();
            log.info("==> 商户查询我的商品列表成功, userId: {}, total: {}", userId, page.getTotal());
            return Response.success(pageResult);
        }
    }

    @Override
    public Response<MerchantGoodsDetailVO> getMyGoodsDetail(Long goodsId) {
        Goods goods = requireOwnedGoods(goodsId);
        GoodsDetailVO base = loadGoodsDetailFromDb(goodsId);
        MerchantGoodsDetailVO vo = MerchantGoodsDetailVO.builder()
                .goodsId(base.getGoodsId())
                .goodsName(base.getGoodsName())
                .goodsImg(base.getGoodsImg())
                .goodsPrice(base.getGoodsPrice())
                .goodsImgList(base.getGoodsImgList())
                .detailContent(base.getDetailContent())
                .skuList(base.getSkuList())
                .categoryId(goods.getCategoryId())
                .shopId(goods.getShopId())
                .status(goods.getStatus())
                .auditStatus(goods.getAuditStatus())
                .build();
        log.info("==> 商户查询我的商品详情成功, goodsId: {}", goodsId);
        return Response.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> updateMyGoods(UpdateGoodsRequest request) {
        Goods goods = requireOwnedGoods(request.getGoodsId());
        assertGoodsEditable(goods);

        Long goodsId = request.getGoodsId();
        LocalDateTime now = LocalDateTime.now();

        Goods updateGoods = Goods.builder()
                .id(goodsId)
                .goodsName(request.getGoodsName())
                .goodsImg(request.getGoodsImg())
                .goodsPrice(request.getGoodsPrice())
                .categoryId(request.getCategoryId())
                .shopId(request.getShopId())
                .updateTime(now)
                .build();
        goodsMapper.updateByPrimaryKeySelective(updateGoods);

        if (request.getDetailContent() != null) {
            GoodsDetail existDetail = goodsDetailMapper.selectByGoodsId(goodsId);
            if (existDetail == null) {
                GoodsDetail detail = GoodsDetail.builder()
                        .id(IdUtil.getSnowflakeNextId())
                        .goodsId(goodsId)
                        .detailContent(request.getDetailContent())
                        .createTime(now)
                        .updateTime(now)
                        .build();
                goodsDetailMapper.insertSelective(detail);
            } else {
                goodsDetailMapper.updateContentByGoodsId(goodsId, request.getDetailContent(), now);
            }
        }

        if (request.getGoodsImgList() != null) {
            goodsImgMapper.deleteByGoodsId(goodsId);
            List<String> goodsImgList = request.getGoodsImgList();
            for (int i = 0; i < goodsImgList.size(); i++) {
                String imgUrl = goodsImgList.get(i);
                if (imgUrl != null && !imgUrl.isEmpty()) {
                    GoodsImg goodsImgEntity = GoodsImg.builder()
                            .id(IdUtil.getSnowflakeNextId())
                            .goodsId(goodsId)
                            .imgUrl(imgUrl)
                            .sort(i)
                            .createTime(now)
                            .build();
                    goodsImgMapper.insertSelective(goodsImgEntity);
                }
            }
        }

        if (request.getSkuList() != null) {
            updateGoodsSkus(goodsId, request.getSkuList(), goods.getGoodsPrice(), now);
        }

        invalidateGoodsCache(goodsId);
        log.info("==> 商户更新商品成功, goodsId: {}", goodsId);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> deleteMyGoods(Long goodsId) {
        Goods goods = requireOwnedGoods(goodsId);
        if (Integer.valueOf(1).equals(goods.getStatus())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "上架中的商品请先下架再删除");
        }

        LocalDateTime now = LocalDateTime.now();
        goodsMapper.updateByPrimaryKeySelective(Goods.builder()
                .id(goodsId)
                .isDelete(1)
                .updateTime(now)
                .build());

        List<GoodsSku> skuList = goodsSkuMapper.selectByGoodsId(goodsId);
        if (skuList != null) {
            for (GoodsSku sku : skuList) {
                goodsSkuMapper.softDeleteById(sku.getId(), now);
            }
        }

        invalidateGoodsCache(goodsId);
        goodsRankRedisService.removeFromRank(goodsId);
        log.info("==> 商户删除商品成功, goodsId: {}", goodsId);
        return Response.success();
    }

    private void updateGoodsSkus(Long goodsId,
                                 List<UpdateGoodsSkuRequest> skuRequests,
                                 java.math.BigDecimal defaultPrice,
                                 LocalDateTime now) {
        List<GoodsSku> existingSkus = goodsSkuMapper.selectByGoodsId(goodsId);
        Set<Long> retainedSkuIds = new HashSet<>();

        for (UpdateGoodsSkuRequest skuReq : skuRequests) {
            if (skuReq.getSkuId() != null) {
                GoodsSku exist = goodsSkuMapper.selectById(skuReq.getSkuId());
                if (exist == null || !goodsId.equals(exist.getGoodsId())) {
                    throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "SKU不存在或不属于该商品");
                }
                GoodsSku skuUpdate = GoodsSku.builder()
                        .id(skuReq.getSkuId())
                        .skuSpecName(skuReq.getSkuSpecName())
                        .skuImg(skuReq.getSkuImg())
                        .marketPrice(skuReq.getMarketPrice() != null ? skuReq.getMarketPrice() : defaultPrice)
                        .salePrice(skuReq.getSalePrice() != null ? skuReq.getSalePrice() : defaultPrice)
                        .stockNum(skuReq.getStockNum())
                        .updateTime(now)
                        .build();
                goodsSkuMapper.updateByPrimaryKeySelective(skuUpdate);
                retainedSkuIds.add(skuReq.getSkuId());
            } else {
                GoodsSku sku = GoodsSku.builder()
                        .id(IdUtil.getSnowflakeNextId())
                        .goodsId(goodsId)
                        .skuSpecName(skuReq.getSkuSpecName() != null ? skuReq.getSkuSpecName() : "")
                        .skuImg(skuReq.getSkuImg() != null ? skuReq.getSkuImg() : "")
                        .marketPrice(skuReq.getMarketPrice() != null ? skuReq.getMarketPrice() : defaultPrice)
                        .salePrice(skuReq.getSalePrice() != null ? skuReq.getSalePrice() : defaultPrice)
                        .stockNum(skuReq.getStockNum() != null ? skuReq.getStockNum() : 0)
                        .salesNum(0)
                        .status(1)
                        .isDelete(0)
                        .createTime(now)
                        .updateTime(now)
                        .build();
                goodsSkuMapper.insertSelective(sku);
                retainedSkuIds.add(sku.getId());
            }
        }

        if (existingSkus != null) {
            for (GoodsSku sku : existingSkus) {
                if (retainedSkuIds.contains(sku.getId())) {
                    continue;
                }
                if (sku.getSalesNum() != null && sku.getSalesNum() > 0) {
                    throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID,
                            "规格【" + sku.getSkuSpecName() + "】已有销量，无法删除");
                }
                goodsSkuMapper.softDeleteById(sku.getId(), now);
            }
        }
    }

    private MerchantGoodsListVO convertMerchantGoodsToVO(Goods goods) {
        List<GoodsSku> skuList = goodsSkuMapper.selectByGoodsId(goods.getId());
        int totalSalesNum = skuList != null ? skuList.stream()
                .filter(sku -> sku.getIsDelete() == 0)
                .mapToInt(sku -> sku.getSalesNum() == null ? 0 : sku.getSalesNum())
                .sum() : 0;

        MerchantGoodsListVO vo = MerchantGoodsListVO.builder()
                .id(goods.getId())
                .goodsName(goods.getGoodsName())
                .goodsImg(goods.getGoodsImg())
                .goodsPrice(goods.getGoodsPrice())
                .salesNum(totalSalesNum)
                .categoryId(goods.getCategoryId())
                .shopId(goods.getShopId())
                .status(goods.getStatus())
                .auditStatus(goods.getAuditStatus())
                .createTime(goods.getCreateTime())
                .build();
        fillMerchantCategoryInfo(vo, goods.getCategoryId());
        return vo;
    }

    private void fillMerchantCategoryInfo(MerchantGoodsListVO vo, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        GoodsCategory category = goodsCategoryMapper.selectByPrimaryKey(categoryId);
        if (category == null) {
            return;
        }
        vo.setCategoryName(category.getCategoryName());
        if (isParentCategory(category)) {
            vo.setParentCategoryId(category.getId());
            vo.setParentCategoryName(category.getCategoryName());
            return;
        }
        vo.setParentCategoryId(category.getParentId());
        GoodsCategory parent = goodsCategoryMapper.selectByPrimaryKey(category.getParentId());
        if (parent != null) {
            vo.setParentCategoryName(parent.getCategoryName());
        }
    }

    private PageResult<MerchantGoodsListVO> buildEmptyMerchantPageResult(int pageNum, int pageSize) {
        return PageResult.<MerchantGoodsListVO>builder()
                .records(Collections.emptyList())
                .total(0L)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(0)
                .build();
    }

    private Goods requireGoods(Long goodsId) {
        if (goodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品ID不能为空");
        }
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null || goods.getIsDelete() == 1) {
            throw new BizException(ResponseCodeEnum.GOODS_NOT_EXIST);
        }
        return goods;
    }

    private Goods requireOwnedGoods(Long goodsId) {
        Goods goods = requireGoods(goodsId);
        assertGoodsOwner(goods);
        return goods;
    }

    private void assertGoodsOwner(Goods goods) {
        if (StpUtil.hasRole("admin")) {
            return;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        if (!userId.equals(goods.getUserId())) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED, "无权操作该商品");
        }
    }

    private void assertGoodsEditable(Goods goods) {
        if (Integer.valueOf(1).equals(goods.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "商品审核中，暂不可编辑");
        }
        if (Integer.valueOf(1).equals(goods.getStatus())
                && GoodsAuditRequest.AUDIT_PASS.equals(goods.getAuditStatus())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "请先下架商品后再编辑");
        }
    }

}