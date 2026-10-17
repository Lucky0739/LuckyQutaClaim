package com.lucky.quota.goods.service.impl;

import com.lucky.quota.common.enums.CollectTypeEnum;
import com.lucky.quota.common.service.CollectRedisService;
import com.lucky.quota.goods.config.GoodsRankProperties;
import com.lucky.quota.goods.enums.GoodsRankSortEnum;
import com.lucky.quota.goods.mapper.GoodsMapper;
import com.lucky.quota.goods.mapper.GoodsSkuMapper;
import com.lucky.quota.goods.model.dto.GoodsAuditRequest;
import com.lucky.quota.goods.model.dto.GoodsRankStatDTO;
import com.lucky.quota.goods.model.entity.Goods;
import com.lucky.quota.goods.service.GoodsRankRedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class GoodsRankRedisServiceImpl implements GoodsRankRedisService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private GoodsSkuMapper goodsSkuMapper;

    @Resource
    private CollectRedisService collectRedisService;

    @Resource
    private GoodsRankProperties goodsRankProperties;

    @Override
    public void updateCollectScore(Long goodsId, int collectNum) {
        if (goodsId == null) {
            return;
        }
        stringRedisTemplate.opsForZSet().add(
                GoodsRankSortEnum.COLLECT.getRedisKey(),
                String.valueOf(goodsId),
                collectNum
        );
    }

    @Override
    public void updateSalesScore(Long goodsId, int salesNum) {
        if (goodsId == null) {
            return;
        }
        stringRedisTemplate.opsForZSet().add(
                GoodsRankSortEnum.SALES.getRedisKey(),
                String.valueOf(goodsId),
                salesNum
        );
    }

    @Override
    public void updateGoodsRankScores(Long goodsId, int collectNum, int salesNum) {
        if (goodsId == null) {
            return;
        }
        String member = String.valueOf(goodsId);
        updateCollectScore(goodsId, collectNum);
        updateSalesScore(goodsId, salesNum);
        double weightScore = goodsRankProperties.calcWeightedScore(collectNum, salesNum);
        stringRedisTemplate.opsForZSet().add(
                GoodsRankSortEnum.WEIGHT.getRedisKey(),
                member,
                weightScore
        );
    }

    @Override
    public void refreshGoodsRankIfOnShelf(Long goodsId) {
        if (goodsId == null) {
            return;
        }
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (!isHomeVisibleGoods(goods)) {
            if (goods == null || goods.getIsDelete() == 1) {
                removeFromRank(goodsId);
            }
            return;
        }
        int collectNum = resolveCollectNum(goodsId, goods.getCollectNum());
        Integer salesNum = goodsSkuMapper.sumSalesNumByGoodsId(goodsId);
        updateGoodsRankScores(goodsId, collectNum, salesNum == null ? 0 : salesNum);
    }

    @Override
    public void removeFromRank(Long goodsId) {
        if (goodsId == null) {
            return;
        }
        String member = String.valueOf(goodsId);
        stringRedisTemplate.opsForZSet().remove(GoodsRankSortEnum.COLLECT.getRedisKey(), member);
        stringRedisTemplate.opsForZSet().remove(GoodsRankSortEnum.SALES.getRedisKey(), member);
        stringRedisTemplate.opsForZSet().remove(GoodsRankSortEnum.WEIGHT.getRedisKey(), member);
    }

    @Override
    public List<Long> listGoodsIdsByRank(GoodsRankSortEnum sortType, long offset, long limit) {
        if (sortType == null || limit <= 0) {
            return Collections.emptyList();
        }
        long end = offset + limit - 1;
        Set<String> members = stringRedisTemplate.opsForZSet()
                .reverseRange(sortType.getRedisKey(), offset, end);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> goodsIds = new ArrayList<>(members.size());
        for (String member : members) {
            goodsIds.add(Long.valueOf(member));
        }
        return goodsIds;
    }

    @Override
    public long countRank(GoodsRankSortEnum sortType) {
        if (sortType == null) {
            return 0L;
        }
        Long size = stringRedisTemplate.opsForZSet().zCard(sortType.getRedisKey());
        return size == null ? 0L : size;
    }

    @Override
    public void rebuildRankFromDb() {
        List<GoodsRankStatDTO> statList = goodsMapper.selectOnShelfGoodsRankStats();
        if (statList == null || statList.isEmpty()) {
            stringRedisTemplate.delete(GoodsRankSortEnum.COLLECT.getRedisKey());
            stringRedisTemplate.delete(GoodsRankSortEnum.SALES.getRedisKey());
            stringRedisTemplate.delete(GoodsRankSortEnum.WEIGHT.getRedisKey());
            log.info("==> 首页物资排行 ZSet 已清空（无上架物资）");
            return;
        }

        String collectKey = GoodsRankSortEnum.COLLECT.getRedisKey();
        String salesKey = GoodsRankSortEnum.SALES.getRedisKey();
        String weightKey = GoodsRankSortEnum.WEIGHT.getRedisKey();
        stringRedisTemplate.delete(collectKey);
        stringRedisTemplate.delete(salesKey);
        stringRedisTemplate.delete(weightKey);

        for (GoodsRankStatDTO stat : statList) {
            Long goodsId = stat.getGoodsId();
            if (goodsId == null) {
                continue;
            }
            int collectNum = resolveCollectNum(goodsId, stat.getCollectNum());
            int salesNum = stat.getSalesNum() == null ? 0 : stat.getSalesNum();
            String member = String.valueOf(goodsId);
            stringRedisTemplate.opsForZSet().add(collectKey, member, collectNum);
            stringRedisTemplate.opsForZSet().add(salesKey, member, salesNum);
            stringRedisTemplate.opsForZSet().add(
                    weightKey,
                    member,
                    goodsRankProperties.calcWeightedScore(collectNum, salesNum)
            );
        }
        log.info("==> 首页物资排行 ZSet 重建完成（含权重）, 物资数: {}, 收藏权重: {}, 销量权重: {}",
                statList.size(), goodsRankProperties.getCollectWeight(), goodsRankProperties.getSalesWeight());
    }

    private boolean isHomeVisibleGoods(Goods goods) {
        return goods != null
                && goods.getIsDelete() == 0
                && Integer.valueOf(1).equals(goods.getStatus())
                && GoodsAuditRequest.AUDIT_PASS.equals(goods.getAuditStatus());
    }

    private int resolveCollectNum(Long goodsId, Integer dbCollectNum) {
        Integer redisCount = collectRedisService.getCollectCount(CollectTypeEnum.GOODS, goodsId);
        if (redisCount != null) {
            return redisCount;
        }
        return dbCollectNum == null ? 0 : dbCollectNum;
    }
}
