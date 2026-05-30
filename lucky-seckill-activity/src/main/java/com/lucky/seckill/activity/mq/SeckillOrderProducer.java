package com.lucky.seckill.activity.mq;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.model.entity.SeckillOrderMessage;
import com.lucky.seckill.common.mq.MqConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SeckillOrderProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public SendResult sendSeckillOrderMessage(SeckillOrderMessage message) {
        message.setMessageId(IdUtil.fastSimpleUUID());
        log.info("==> 发送秒杀下单消息, messageId={}, orderNo={}, userId={}",
                message.getMessageId(), message.getOrderNo(), message.getUserId());

        SendResult result = rocketMQTemplate.syncSend(
                String.format("%s:%s", MqConstants.SECKILL_ORDER_TOPIC, MqConstants.SECKILL_ORDER_TAG),
                message
        );

        log.info("==> 秒杀下单消息发送成功, messageId={}, orderNo={}, result={}",
                message.getMessageId(), message.getOrderNo(), result);
        return result;
    }
}
