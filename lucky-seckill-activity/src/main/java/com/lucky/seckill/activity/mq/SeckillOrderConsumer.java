package com.lucky.seckill.activity.mq;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.activity.enums.SeckillOrderStatusEnum;
import com.lucky.seckill.activity.mapper.SeckillGoodsMapper;
import com.lucky.seckill.activity.mapper.SeckillOrderMapper;
import com.lucky.seckill.activity.model.entity.SeckillGoods;
import com.lucky.seckill.activity.model.entity.SeckillOrder;
import com.lucky.seckill.activity.service.SeckillRedisService;
import com.lucky.seckill.common.model.entity.SeckillOrderMessage;
import com.lucky.seckill.common.mq.MqConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 秒杀下单消息消费者：异步落库并扣减数据库库存
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MqConstants.SECKILL_ORDER_TOPIC,
        selectorExpression = MqConstants.SECKILL_ORDER_TAG,
        consumerGroup = "seckill-order-consumer-group"
)
public class SeckillOrderConsumer implements RocketMQListener<SeckillOrderMessage> {

    @Resource
    private SeckillOrderMapper seckillOrderMapper;

    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private SeckillRedisService seckillRedisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(SeckillOrderMessage message) {
        log.info("==> 收到秒杀下单消息, messageId={}, orderNo={}, userId={}",
                message.getMessageId(), message.getOrderNo(), message.getUserId());

        SeckillOrder existOrder = seckillOrderMapper.selectByOrderNo(message.getOrderNo());
        if (existOrder != null) {
            log.warn("==> 秒杀订单已存在，跳过, orderNo={}", message.getOrderNo());
            return;
        }

        SeckillGoods seckillGoods = seckillGoodsMapper.selectByPrimaryKey(message.getSeckillGoodsId());
        if (seckillGoods == null) {
            log.error("==> 秒杀商品不存在，回滚 Redis 库存, seckillGoodsId={}", message.getSeckillGoodsId());
            rollbackRedis(message);
            throw new RuntimeException("秒杀商品不存在");
        }

        int deductRows = seckillGoodsMapper.deductStock(
                seckillGoods.getId(),
                message.getBuyNum(),
                seckillGoods.getVersion(),
                LocalDateTime.now()
        );
        if (deductRows <= 0) {
            log.error("==> 数据库库存扣减失败，回滚 Redis 库存, orderNo={}", message.getOrderNo());
            rollbackRedis(message);
            throw new RuntimeException("数据库库存扣减失败");
        }

        LocalDateTime now = LocalDateTime.now();
        SeckillOrder order = SeckillOrder.builder()
                .id(IdUtil.getSnowflakeNextId())
                .userId(message.getUserId())
                .activityId(message.getActivityId())
                .goodsId(message.getGoodsId())
                .skuId(message.getSkuId())
                .orderNo(message.getOrderNo())
                .seckillPrice(message.getSeckillPrice())
                .goodsName(message.getGoodsName())
                .goodsImg(message.getGoodsImg())
                .status(SeckillOrderStatusEnum.PENDING_PAYMENT.getCode())
                .expireTime(message.getExpireTime())
                .isDeleted(0)
                .createTime(now)
                .updateTime(now)
                .build();

        seckillOrderMapper.insertSelective(order);

        log.info("==> 秒杀订单创建成功, orderNo={}, orderId={}", message.getOrderNo(), order.getId());
    }

    private void rollbackRedis(SeckillOrderMessage message) {
        seckillRedisService.rollbackStock(
                message.getActivityId(),
                message.getUserId(),
                message.getGoodsId(),
                message.getSkuId(),
                message.getBuyNum(),
                message.getOrderNo()
        );
    }
}
