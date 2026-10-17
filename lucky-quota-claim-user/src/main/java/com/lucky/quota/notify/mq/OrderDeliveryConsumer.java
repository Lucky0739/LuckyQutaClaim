package com.lucky.quota.notify.mq;

import cn.hutool.core.util.IdUtil;
import com.lucky.quota.common.constant.NotifyTypeConstants;
import com.lucky.quota.common.model.entity.OrderDeliveryMessage;
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
 * 申领单发放消息消费者
 * 收到发放消息后，向买家发送站内通知
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MqConstants.ORDER_DELIVERY_TOPIC,
        selectorExpression = MqConstants.ORDER_DELIVERY_TAG,
        consumerGroup = "order-delivery-consumer-group"
)
public class OrderDeliveryConsumer implements RocketMQListener<OrderDeliveryMessage> {

    @Resource
    private NotifyService notifyService;

    @Override
    public void onMessage(OrderDeliveryMessage message) {
        try {
            log.info("==> 收到申领单发放消息, messageId: {}, orderNo: {}, buyerUserId: {}",
                    message.getMessageId(), message.getOrderNo(), message.getBuyerUserId());

            String goodsName = message.getGoodsName() == null ? "物资" : message.getGoodsName();
            Notify notify = Notify.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .sendUserId(message.getSellerUserId())
                    .receiveUserId(message.getBuyerUserId())
                    .title("发放通知")
                    .content(String.format("您的申领单【%s】物资【%s】已发放，请注意查收", message.getOrderNo(), goodsName))
                    .notifyType(NotifyTypeConstants.ORDER_DELIVERY)
                    .businessId(message.getOrderId())
                    .subType(0)
                    .isRead(0)
                    .isDelete(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            notifyService.saveNotify(notify);

            log.info("==> 发放通知已发送, messageId: {}, orderNo: {}, buyerUserId: {}, notifyId: {}",
                    message.getMessageId(), message.getOrderNo(), message.getBuyerUserId(), notify.getId());
        } catch (Exception e) {
            log.error("==> 处理申领单发放消息失败, messageId: {}, orderNo: {}, error: {}",
                    message.getMessageId(), message.getOrderNo(), e.getMessage(), e);
            throw new RuntimeException("处理申领单发放消息失败", e);
        }
    }
}
