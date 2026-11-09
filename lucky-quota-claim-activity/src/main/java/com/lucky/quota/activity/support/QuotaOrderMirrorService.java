package com.lucky.quota.activity.support;

import cn.hutool.core.util.IdUtil;
import com.lucky.quota.activity.enums.QuotaOrderStatusEnum;
import com.lucky.quota.activity.mapper.OrderMirrorMapper;
import com.lucky.quota.activity.mapper.QuotaOrderMapper;
import com.lucky.quota.activity.model.entity.MirroredOrderRow;
import com.lucky.quota.activity.model.entity.QuotaOrder;
import com.lucky.quota.common.exception.RetryableException;
import com.lucky.quota.common.model.entity.QuotaOrderMessage;
import com.lucky.quota.common.utils.Response;
import com.lucky.quota.goods.model.entity.Goods;
import com.lucky.quota.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 限量申领单写入 t_seckill_order 后，镜像到同一库的 t_order（供「我的申领」展示）。
 */
@Component
@Slf4j
public class QuotaOrderMirrorService {

    private static final int PAY_TIMEOUT_MINUTES = 10;
    private static final int MAX_GOODS_IMG_LEN = 255;

    private static final String PLACEHOLDER_MOBILE = "10000000000";
    private static final String PLACEHOLDER_TEXT = "待填写";

    @Resource
    private OrderMirrorMapper orderMirrorMapper;

    @Resource
    private QuotaOrderMapper quotaOrderMapper;

    @Resource
    private GoodsService goodsService;

    public void mirrorToOrderService(QuotaOrderMessage message) {
        String orderNo = message.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            throw new RetryableException("orderNo 为空，无法镜像 t_order", orderNo);
        }

        QuotaOrder seckill = quotaOrderMapper.selectByOrderNo(orderNo);
        if (orderMirrorMapper.countByOrderNo(orderNo) > 0) {
            if (seckill != null) {
                repairExistingMirror(seckill, message);
            }
            log.info("==> t_order 已存在限量单，已尝试修复镜像字段, orderNo={}", orderNo);
            return;
        }

        int buyNum = message.getBuyNum() == null || message.getBuyNum() <= 0 ? 1 : message.getBuyNum();
        if (message.getSeckillPrice() == null) {
            throw new RetryableException("镜像参数不完整", orderNo);
        }

        MirrorSnapshot snapshot = buildSnapshot(seckill, message, buyNum);

        MirroredOrderRow row = MirroredOrderRow.builder()
                .id(IdUtil.getSnowflakeNextId())
                .userId(message.getUserId())
                .goodsId(message.getGoodsId())
                .shopId(snapshot.shopId())
                .skuId(message.getSkuId())
                .orderNo(orderNo)
                .goodsPrice(message.getSeckillPrice())
                .buyGoodsNum(buyNum)
                .totalAmount(snapshot.totalAmount())
                .goodsName(snapshot.goodsName())
                .goodsImg(snapshot.goodsImg())
                .status(QuotaOrderStatusEnum.PENDING_PAYMENT.getCode())
                .mobile(PLACEHOLDER_MOBILE)
                .receiver(PLACEHOLDER_TEXT)
                .province(PLACEHOLDER_TEXT)
                .city(PLACEHOLDER_TEXT)
                .area(PLACEHOLDER_TEXT)
                .detailAddress(PLACEHOLDER_TEXT)
                .expireTime(snapshot.expireTime())
                .isDelete(0)
                .createTime(snapshot.createTime())
                .updateTime(LocalDateTime.now())
                .build();

        try {
            orderMirrorMapper.insertMirroredOrder(row);
            log.info("==> 限量单已镜像 t_order（本地库）, orderNo={}", orderNo);
        } catch (Exception e) {
            if (orderMirrorMapper.countByOrderNo(orderNo) > 0) {
                if (seckill != null) {
                    repairExistingMirror(seckill, message);
                }
                return;
            }
            log.error("==> 镜像 t_order 失败, orderNo={}", orderNo, e);
            throw new RetryableException("镜像 t_order 失败: " + e.getMessage(), orderNo, e);
        }
    }

    public boolean mirrorFromSeckillOrder(String orderNo) {
        QuotaOrder quotaOrder = quotaOrderMapper.selectByOrderNo(orderNo);
        if (quotaOrder == null) {
            return false;
        }
        QuotaOrderMessage message = QuotaOrderMessage.builder()
                .userId(quotaOrder.getUserId())
                .activityId(quotaOrder.getActivityId())
                .goodsId(quotaOrder.getGoodsId())
                .skuId(quotaOrder.getSkuId())
                .orderNo(quotaOrder.getOrderNo())
                .seckillPrice(quotaOrder.getSeckillPrice())
                .goodsName(quotaOrder.getGoodsName())
                .goodsImg(quotaOrder.getGoodsImg())
                .buyNum(1)
                .expireTime(quotaOrder.getExpireTime())
                .build();
        mirrorToOrderService(message);
        return true;
    }

    private void repairExistingMirror(QuotaOrder seckill, QuotaOrderMessage message) {
        MirrorSnapshot snapshot = buildSnapshot(seckill, message,
                message.getBuyNum() == null || message.getBuyNum() <= 0 ? 1 : message.getBuyNum());
        MirroredOrderRow patch = MirroredOrderRow.builder()
                .orderNo(seckill.getOrderNo())
                .goodsName(snapshot.goodsName())
                .goodsImg(snapshot.goodsImg())
                .createTime(snapshot.createTime())
                .expireTime(snapshot.expireTime())
                .updateTime(LocalDateTime.now())
                .build();
        int rows = orderMirrorMapper.updateMirroredOrder(patch);
        if (rows > 0) {
            log.info("==> 已修复 t_order 镜像字段, orderNo={}, expireTime={}", seckill.getOrderNo(), snapshot.expireTime());
        }
    }

    private MirrorSnapshot buildSnapshot(QuotaOrder seckill, QuotaOrderMessage message, int buyNum) {
        Long shopId = resolveShopId(message.getGoodsId());
        BigDecimal totalAmount = message.getSeckillPrice().multiply(BigDecimal.valueOf(buyNum));

        String goodsName = firstNonBlank(
                seckill != null ? seckill.getGoodsName() : null,
                message.getGoodsName(),
                "限量物资");
        String goodsImg = truncateImg(resolveGoodsImg(seckill, message));

        LocalDateTime createTime;
        LocalDateTime expireTime;
        if (seckill != null && seckill.getCreateTime() != null) {
            createTime = seckill.getCreateTime();
            expireTime = seckill.getExpireTime() != null
                    ? seckill.getExpireTime()
                    : createTime.plusMinutes(PAY_TIMEOUT_MINUTES);
        } else if (message.getExpireTime() != null) {
            expireTime = message.getExpireTime();
            createTime = expireTime.minusMinutes(PAY_TIMEOUT_MINUTES);
        } else {
            LocalDateTime now = LocalDateTime.now();
            createTime = now;
            expireTime = now.plusMinutes(PAY_TIMEOUT_MINUTES);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean pending = seckill == null
                || QuotaOrderStatusEnum.PENDING_PAYMENT.getCode().equals(seckill.getStatus());
        if (pending && expireTime != null && !now.isBefore(expireTime)) {
            // 晚镜像 / 补数：用户在「我的申领」看到时仍应保留支付窗口
            expireTime = now.plusMinutes(PAY_TIMEOUT_MINUTES);
        }

        return new MirrorSnapshot(shopId, totalAmount, goodsName, goodsImg, createTime, expireTime);
    }

    private String resolveGoodsImg(QuotaOrder seckill, QuotaOrderMessage message) {
        if (seckill != null && isUsableImg(seckill.getGoodsImg())) {
            return seckill.getGoodsImg().trim();
        }
        if (message.getGoodsImg() != null && isUsableImg(message.getGoodsImg())) {
            return message.getGoodsImg().trim();
        }
        if (message.getGoodsId() != null) {
            Response<Goods> goodsResponse = goodsService.getGoodsById(message.getGoodsId());
            if (goodsResponse != null && goodsResponse.isSuccess() && goodsResponse.getData() != null) {
                String img = goodsResponse.getData().getGoodsImg();
                if (isUsableImg(img)) {
                    return img.trim();
                }
            }
        }
        return "-";
    }

    private static boolean isUsableImg(String img) {
        if (img == null || img.isBlank() || "-".equals(img.trim())) {
            return false;
        }
        return !img.contains("/test");
    }

    private static String truncateImg(String img) {
        if (img == null) {
            return "-";
        }
        return img.length() <= MAX_GOODS_IMG_LEN ? img : img.substring(0, MAX_GOODS_IMG_LEN);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "限量物资";
    }

    private Long resolveShopId(Long goodsId) {
        if (goodsId == null) {
            return null;
        }
        Response<Goods> goodsResponse = goodsService.getGoodsById(goodsId);
        if (goodsResponse != null && goodsResponse.isSuccess() && goodsResponse.getData() != null) {
            return goodsResponse.getData().getShopId();
        }
        return null;
    }

    private record MirrorSnapshot(
            Long shopId,
            BigDecimal totalAmount,
            String goodsName,
            String goodsImg,
            LocalDateTime createTime,
            LocalDateTime expireTime) {
    }
}
