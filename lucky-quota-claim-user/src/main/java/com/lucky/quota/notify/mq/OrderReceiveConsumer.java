package com.lucky.quota.notify.mq;

import cn.hutool.core.util.IdUtil;
import com.lucky.quota.common.constant.NotifyTypeConstants;
import com.lucky.quota.common.model.entity.OrderReceiveMessage;
import com.lucky.quota.common.mq.MqConstants;
import com.lucky.quota.notify.model.entity.Notify;
import com.lucky.quota.notify.service.NotifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 申领单确认到货消息消费者
 * 收到消息后，向商家发送站内通知
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MqConstants.ORDER_RECEIVE_TOPIC,
        selectorExpression = MqConstants.ORDER_RECEIVE_TAG,
        consumerGroup = "order-receive-consumer-group"
)
public class OrderReceiveConsumer implements RocketMQListener<OrderReceiveMessage> {

    @Resource
    private NotifyService notifyService;

    @Override
    public void onMessage(OrderReceiveMessage message) {
        try {
            log.info("==> 收到申领单确认到货消息, messageId: {}, orderNo: {}, sellerUserId: {}",
                    message.getMessageId(), message.getOrderNo(), message.getSellerUserId());

            if (message.getSellerUserId() == null) {
                log.warn("==> 商家用户ID为空，跳过确认到货通知, orderNo: {}", message.getOrderNo());
                return;
            }

            String goodsName = message.getGoodsName() == null ? "物资" : message.getGoodsName();
            Notify notify = Notify.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .sendUserId(message.getBuyerUserId())
                    .receiveUserId(message.getSellerUserId())
                    .title("买家已确认到货")
                    .content(String.format("买家已确认到货，申领单【%s】物资【%s】", message.getOrderNo(), goodsName))
                    .notifyType(NotifyTypeConstants.ORDER_RECEIVE)
                    .businessId(message.getOrderId())
                    .subType(0)
                    .isRead(0)
                    .isDelete(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            notifyService.saveNotify(notify);

            log.info("==> 确认到货通知已发送, messageId: {}, orderNo: {}, sellerUserId: {}, notifyId: {}",
                    message.getMessageId(), message.getOrderNo(), message.getSellerUserId(), notify.getId());
        } catch (Exception e) {
            log.error("==> 处理申领单确认到货消息失败, messageId: {}, orderNo: {}, error: {}",
                    message.getMessageId(), message.getOrderNo(), e.getMessage(), e);
            throw new RuntimeException("处理申领单确认到货消息失败", e);
        }
    }
}
