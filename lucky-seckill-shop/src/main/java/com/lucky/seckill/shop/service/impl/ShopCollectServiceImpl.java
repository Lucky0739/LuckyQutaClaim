package com.lucky.seckill.shop.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.enums.CollectTypeEnum;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.service.CollectRedisService;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.shop.mapper.ShopCollectMapper;
import com.lucky.seckill.shop.mapper.ShopMapper;
import com.lucky.seckill.shop.model.dto.ShopCollectRequest;
import com.lucky.seckill.shop.model.entity.Shop;
import com.lucky.seckill.shop.model.entity.ShopCollect;
import com.lucky.seckill.shop.model.vo.ShopCollectStatusVO;
import com.lucky.seckill.shop.model.vo.ShopCollectVO;
import com.lucky.seckill.shop.service.ShopCollectService;
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
public class ShopCollectServiceImpl implements ShopCollectService {

    private static final int SYNC_BATCH_SIZE = 100;

    @Resource
    private ShopMapper shopMapper;

    @Resource
    private ShopCollectMapper shopCollectMapper;

    @Resource
    private CollectRedisService collectRedisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> collectShop(ShopCollectRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long shopId = request.getShopId();
        log.info("==> 收藏店铺, userId: {}, shopId: {}", userId, shopId);

        Shop shop = shopMapper.selectByPrimaryKey(shopId);
        if (shop == null) {
            throw new BizException(ResponseCodeEnum.SHOP_NOT_EXIST);
        }

        int initialCount = shop.getCollectNum() != null ? shop.getCollectNum() : 0;
        int result = collectRedisService.addCollect(CollectTypeEnum.SHOP, userId, shopId, initialCount);
        if (result == -1) {
            throw new BizException(ResponseCodeEnum.COLLECT_ALREADY_EXISTS);
        }

        ShopCollect exist = shopCollectMapper.selectByUserIdAndShopIdIgnoreDelete(userId, shopId);
        if (exist == null) {
            ShopCollect collect = ShopCollect.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .userId(userId)
                    .shopId(shopId)
                    .isDelete(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            shopCollectMapper.insertSelective(collect);
        } else if (exist.getIsDelete() != null && exist.getIsDelete() == 1) {
            shopCollectMapper.restoreByUserIdAndShopId(userId, shopId);
        }

        log.info("==> 收藏店铺成功, userId: {}, shopId: {}", userId, shopId);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> cancelCollect(ShopCollectRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long shopId = request.getShopId();
        log.info("==> 取消收藏店铺, userId: {}, shopId: {}", userId, shopId);

        int result = collectRedisService.cancelCollect(CollectTypeEnum.SHOP, userId, shopId);
        if (result == -1) {
            throw new BizException(ResponseCodeEnum.COLLECT_NOT_EXISTS);
        }

        shopCollectMapper.softDeleteByUserIdAndShopId(userId, shopId);
        log.info("==> 取消收藏店铺成功, userId: {}, shopId: {}", userId, shopId);
        return Response.success();
    }

    @Override
    public Response<ShopCollectStatusVO> getCollectStatus(Long shopId) {
        if (shopId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "店铺ID不能为空");
        }

        Shop shop = shopMapper.selectByPrimaryKey(shopId);
        if (shop == null) {
            throw new BizException(ResponseCodeEnum.SHOP_NOT_EXIST);
        }

        boolean collected = false;
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            collected = collectRedisService.isCollected(CollectTypeEnum.SHOP, userId, shopId);
        }

        Integer redisCount = collectRedisService.getCollectCount(CollectTypeEnum.SHOP, shopId);
        int collectNum = redisCount != null ? redisCount : (shop.getCollectNum() != null ? shop.getCollectNum() : 0);

        ShopCollectStatusVO vo = ShopCollectStatusVO.builder()
                .shopId(shopId)
                .collected(collected)
                .collectNum(collectNum)
                .build();
        return Response.success(vo);
    }

    @Override
    public Response<List<ShopCollectVO>> getMyCollectList() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 查询我的店铺收藏列表, userId: {}", userId);

        List<ShopCollectVO> list = shopCollectMapper.selectCollectVOListByUserId(userId);
        if (list == null || list.isEmpty()) {
            return Response.success(Collections.emptyList());
        }
        return Response.success(list);
    }

    @Override
    public void syncCollectCountToDb() {
        Set<String> shopIds = collectRedisService.popSyncIds(CollectTypeEnum.SHOP, SYNC_BATCH_SIZE);
        if (shopIds.isEmpty()) {
            return;
        }

        for (String shopIdStr : shopIds) {
            try {
                Long shopId = Long.parseLong(shopIdStr);
                Integer count = collectRedisService.getCollectCount(CollectTypeEnum.SHOP, shopId);
                if (count == null) {
                    continue;
                }
                shopMapper.updateCollectNum(shopId, count);
                log.info("==> 同步店铺收藏数到MySQL, shopId: {}, collectNum: {}", shopId, count);
            } catch (Exception e) {
                log.error("==> 同步店铺收藏数失败, shopId: {}", shopIdStr, e);
            }
        }
    }
}
