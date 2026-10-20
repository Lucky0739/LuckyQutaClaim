package com.lucky.quota.order.mq;

import cn.hutool.core.util.IdUtil;
import com.lucky.quota.common.model.entity.OrderReceiveMessage;
import com.lucky.quota.common.mq.MqConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderReceiveProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void sendOrderReceiveMessage(OrderReceiveMessage message) {
        try {
            message.setMessageId(IdUtil.fastSimpleUUID());
            log.info("==> 准备发送申领单确认到货消息, messageId: {}, orderNo: {}, buyerUserId: {}",
                    message.getMessageId(), message.getOrderNo(), message.getBuyerUserId());

            rocketMQTemplate.asyncSend(
                    String.format("%s:%s", MqConstants.ORDER_RECEIVE_TOPIC, MqConstants.ORDER_RECEIVE_TAG),
                    message,
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("==> 申领单确认到货消息发送成功, messageId: {}, orderNo: {}, sendResult: {}",
                                    message.getMessageId(), message.getOrderNo(), sendResult);
                        }

                        @Override
                        public void onException(Throwable e) {
                            log.error("==> 申领单确认到货消息发送失败, messageId: {}, orderNo: {}, error: {}",
                                    message.getMessageId(), message.getOrderNo(), e.getMessage(), e);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("==> 发送申领单确认到货消息异常, orderNo: {}, buyerUserId: {}, error: {}",
                    message.getOrderNo(), message.getBuyerUserId(), e.getMessage(), e);
            throw new RuntimeException("发送申领单确认到货消息失败", e);
        }
    }
}
