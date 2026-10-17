package com.lucky.quota.goods.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.lucky.quota.common.enums.CollectTypeEnum;
import com.lucky.quota.common.enums.ResponseCodeEnum;
import com.lucky.quota.common.exception.BizException;
import com.lucky.quota.common.service.CollectRedisService;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.mapper.GoodsCollectMapper;
import com.lucky.quota.goods.mapper.GoodsMapper;
import com.lucky.quota.goods.model.dto.GoodsCollectRequest;
import com.lucky.quota.goods.model.entity.Goods;
import com.lucky.quota.goods.model.entity.GoodsCollect;
import com.lucky.quota.goods.model.vo.GoodsCollectStatusVO;
import com.lucky.quota.goods.model.vo.GoodsCollectVO;
import com.lucky.quota.goods.service.GoodsCollectService;
import com.lucky.quota.goods.service.GoodsRankRedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class GoodsCollectServiceImpl implements GoodsCollectService {

    private static final int SYNC_BATCH_SIZE = 100;

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private GoodsCollectMapper goodsCollectMapper;

    @Resource
    private CollectRedisService collectRedisService;

    @Resource
    private GoodsRankRedisService goodsRankRedisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> collectGoods(GoodsCollectRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long goodsId = request.getGoodsId();
        log.info("==> 收藏物资, userId: {}, goodsId: {}", userId, goodsId);

        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null) {
            throw new BizException(ResponseCodeEnum.GOODS_NOT_EXIST);
        }

        int initialCount = goods.getCollectNum() != null ? goods.getCollectNum() : 0;
        int result = collectRedisService.addCollect(CollectTypeEnum.GOODS, userId, goodsId, initialCount);
        if (result == -1) {
            throw new BizException(ResponseCodeEnum.COLLECT_ALREADY_EXISTS);
        }

        GoodsCollect exist = goodsCollectMapper.selectByUserIdAndGoodsId(userId, goodsId);
        if (exist == null) {
            GoodsCollect collect = GoodsCollect.builder()
                    .userId(userId)
                    .goodsId(goodsId)
                    .createTime(LocalDateTime.now())
                    .build();
            int rows = goodsCollectMapper.insertSelective(collect);
            if (rows == 0) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "收藏记录保存失败");
            }
        }

        goodsRankRedisService.refreshGoodsRankIfOnShelf(goodsId);
        log.info("==> 收藏物资成功, userId: {}, goodsId: {}", userId, goodsId);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> cancelCollect(GoodsCollectRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long goodsId = request.getGoodsId();
        log.info("==> 取消收藏物资, userId: {}, goodsId: {}", userId, goodsId);

        int result = collectRedisService.cancelCollect(CollectTypeEnum.GOODS, userId, goodsId);
        if (result == -1) {
            throw new BizException(ResponseCodeEnum.COLLECT_NOT_EXISTS);
        }

        goodsCollectMapper.deleteByUserIdAndGoodsId(userId, goodsId);
        goodsRankRedisService.refreshGoodsRankIfOnShelf(goodsId);
        log.info("==> 取消收藏物资成功, userId: {}, goodsId: {}", userId, goodsId);
        return Response.success();
    }

    @Override
    public Response<GoodsCollectStatusVO> getCollectStatus(Long goodsId) {
        if (goodsId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "物资ID不能为空");
        }

        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (goods == null) {
            throw new BizException(ResponseCodeEnum.GOODS_NOT_EXIST);
        }

        boolean collected = false;
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            collected = collectRedisService.isCollected(CollectTypeEnum.GOODS, userId, goodsId);
        }

        Integer redisCount = collectRedisService.getCollectCount(CollectTypeEnum.GOODS, goodsId);
        int collectNum = redisCount != null ? redisCount : (goods.getCollectNum() != null ? goods.getCollectNum() : 0);

        GoodsCollectStatusVO vo = GoodsCollectStatusVO.builder()
                .goodsId(goodsId)
                .collected(collected)
                .collectNum(collectNum)
                .build();
        return Response.success(vo);
    }

    @Override
    public Response<List<GoodsCollectVO>> getMyCollectList() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 查询我的物资收藏列表, userId: {}", userId);

        List<GoodsCollectVO> list = goodsCollectMapper.selectCollectVOListByUserId(userId);
        if (list == null || list.isEmpty()) {
            return Response.success(Collections.emptyList());
        }
        return Response.success(list);
    }

    @Override
    public void syncCollectCountToDb() {
        Set<String> goodsIds = collectRedisService.popSyncIds(CollectTypeEnum.GOODS, SYNC_BATCH_SIZE);
        if (goodsIds.isEmpty()) {
            return;
        }

        for (String goodsIdStr : goodsIds) {
            try {
                Long goodsId = Long.parseLong(goodsIdStr);
                Integer count = collectRedisService.getCollectCount(CollectTypeEnum.GOODS, goodsId);
                if (count == null) {
                    continue;
                }
                goodsMapper.updateCollectNum(goodsId, count, LocalDateTime.now());
                goodsRankRedisService.refreshGoodsRankIfOnShelf(goodsId);
                log.info("==> 同步物资收藏数到MySQL, goodsId: {}, collectNum: {}", goodsId, count);
            } catch (Exception e) {
                log.error("==> 同步物资收藏数失败, goodsId: {}", goodsIdStr, e);
            }
        }
    }
}
