package com.lucky.quota.shop.mq;

import cn.hutool.core.util.IdUtil;
import com.lucky.quota.common.mq.MqConstants;
import com.lucky.quota.common.model.entity.MerchantApplyMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
@Slf4j
public class MerchantApplyProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void sendMerchantApplyMessage(MerchantApplyMessage merchantApplyMessage) {
        try {
            merchantApplyMessage.setMessageId(IdUtil.fastSimpleUUID());
            log.info("==> 准备发送商家申请消息, messageId: {}, shopId: {}, userId: {}",
                    merchantApplyMessage.getMessageId(), merchantApplyMessage.getShopId(), merchantApplyMessage.getUserId());

            rocketMQTemplate.asyncSend(
                    String.format("%s:%s", MqConstants.MERCHANT_APPLY_TOPIC, MqConstants.MERCHANT_APPLY_TAG),
                    merchantApplyMessage,
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("==> 商家申请消息发送成功, messageId: {}, sendResult: {}",
                                    merchantApplyMessage.getMessageId(), sendResult);
                        }

                        @Override
                        public void onException(Throwable e) {
                            log.error("==> 商家申请消息发送失败, messageId: {}, error: {}",
                                    merchantApplyMessage.getMessageId(), e.getMessage(), e);
                        }
                    }
            );

            log.info("==> 商家申请消息发送中, messageId: {}", merchantApplyMessage.getMessageId());
        } catch (Exception e) {
            log.error("==> 发送商家申请消息异常, shopId: {}, userId: {}, error: {}",
                    merchantApplyMessage.getShopId(), merchantApplyMessage.getUserId(), e.getMessage(), e);
            throw new RuntimeException("发送商家申请消息失败", e);
        }
    }

    public SendResult sendMerchantApplyMessageSync(MerchantApplyMessage message) {
        try {
            message.setMessageId(IdUtil.fastSimpleUUID());
            log.info("==> 同步发送商家申请消息, messageId: {}, shopId: {}, userId: {}",
                    message.getMessageId(), message.getShopId(), message.getUserId());

            SendResult result = rocketMQTemplate.syncSend(
                    String.format("%s:%s", MqConstants.MERCHANT_APPLY_TOPIC, MqConstants.MERCHANT_APPLY_TAG),
                    message
            );

            log.info("==> 商家申请消息同步发送成功, messageId: {}, result: {}",
                    message.getMessageId(), result);
            return result;
        } catch (Exception e) {
            log.error("==> 同步发送商家申请消息失败, shopId: {}, userId: {}, error: {}",
                    message.getShopId(), message.getUserId(), e.getMessage(), e);
            throw new RuntimeException("同步发送商家申请消息失败", e);
        }
    }
}