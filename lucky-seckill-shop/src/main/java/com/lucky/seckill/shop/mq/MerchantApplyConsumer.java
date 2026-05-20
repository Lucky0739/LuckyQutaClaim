package com.lucky.seckill.shop.mq;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.model.entity.MerchantApplyMessage;
import com.lucky.seckill.common.model.entity.Notify;
import com.lucky.seckill.notify.feign.NotifyFeignClient;
import com.lucky.seckill.common.mq.MerchantApplyProducer;
import com.lucky.seckill.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


/**
 * 商家申请消息消费者
 * 只在审核通过时发送通知给申请开店的用户
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MerchantApplyProducer.MERCHANT_APPLY_TOPIC,
        selectorExpression = MerchantApplyProducer.MERCHANT_APPLY_TAG,
        consumerGroup = "merchant-apply-consumer-group"
)
public class MerchantApplyConsumer implements RocketMQListener<MerchantApplyMessage> {
    @Resource
    private NotifyFeignClient notifyFeignClient;


    @Override
    public void onMessage(MerchantApplyMessage message) {
        try {
            log.info("==> 收到商家申请审核结果消息, messageId: {}, shopId: {}, userId: {}, status: {}",
                    message.getMessageId(),
                    message.getShopId(),
                    message.getUserId(),
                    message.getStatus());

            // 只有审核通过时才发送通知
            if (message.getStatus() != null && message.getStatus() == 1) {
                Notify notify = Notify.builder()
                        .id(IdUtil.getSnowflakeNextId())
                        .sendUserId(5L)
                        .receiveUserId(message.getUserId())
                        .title("开店申请已通过")
                        .content(String.format("恭喜！您的店铺【%s】开店申请已通过审核！", message.getShopName()))
                        .notifyType(4)
                        .businessId(message.getShopId())
                        .subType(0)
                        .isRead(0)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDelete(0)
                        .build();

                Response<Boolean> response = notifyFeignClient.saveNotify(notify);
                if (response == null || !response.isSuccess()) {
                    throw new RuntimeException("保存审核通过通知失败");
                }

                log.info("==> 审核通过通知已发送, messageId: {}, shopId: {}, userId: {}, notifyId: {}",
                        message.getMessageId(), message.getShopId(), message.getUserId(), notify.getId());
            } else {
                log.info("==> 审核未通过或状态不符，不发送通知, messageId: {}, status: {}",
                        message.getMessageId(), message.getStatus());
            }
        } catch (Exception e) {
            log.error("==> 处理商家申请审核结果消息失败, messageId: {}, error: {}",
                    message.getMessageId(), e.getMessage(), e);
            throw new RuntimeException("处理商家申请审核结果消息失败", e);
        }
    }
}
