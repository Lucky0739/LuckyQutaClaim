package com.lucky.quota.activity.mq;

import cn.hutool.core.util.IdUtil;
import com.lucky.quota.common.model.entity.QuotaOrderMessage;
import com.lucky.quota.common.mq.MqConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class QuotaOrderProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public SendResult sendQuotaOrderMessage(QuotaOrderMessage message) {
        message.setMessageId(IdUtil.fastSimpleUUID());
        log.info("==> 发送限量申领消息, messageId={}, orderNo={}, userId={}",
                message.getMessageId(), message.getOrderNo(), message.getUserId());

        SendResult result = rocketMQTemplate.syncSend(
                String.format("%s:%s", MqConstants.QUOTA_ORDER_TOPIC, MqConstants.QUOTA_ORDER_TAG),
                message
        );

        log.info("==> 限量申领消息发送成功, messageId={}, orderNo={}, result={}",
                message.getMessageId(), message.getOrderNo(), result);
        return result;
    }
}
