package com.lucky.seckill.user.mq;

import com.lucky.seckill.common.mq.MqConstants;
import com.lucky.seckill.common.model.entity.MerchantApplyMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 商家申请消息消费者
 * 负责接收商家申请消息并记录日志
 * 实际审批流程由管理员接口触发
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MqConstants.MERCHANT_APPLY_TOPIC,
        selectorExpression = MqConstants.MERCHANT_APPLY_TAG,
        consumerGroup = "merchant-apply-consumer-group"
)
public class MerchantApplyConsumer implements RocketMQListener<MerchantApplyMessage> {

    @Override
    public void onMessage(MerchantApplyMessage merchantApplyMessage) {
        log.info("==> 收到商家申请消息, messageId: {}, shopId: {}, userId: {}, shopName: {}",
                merchantApplyMessage.getMessageId(),
                merchantApplyMessage.getShopId(),
                merchantApplyMessage.getUserId(),
                merchantApplyMessage.getShopName());

        log.info("==> 商家申请已提交，等待管理员审批...");

        log.info("==> 消息处理完成, messageId: {}", merchantApplyMessage.getMessageId());
    }
}
