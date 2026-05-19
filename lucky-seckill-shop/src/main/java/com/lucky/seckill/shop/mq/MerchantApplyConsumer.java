package com.lucky.seckill.shop.mq;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.feign.NotifyFeignClient;
import com.lucky.seckill.common.model.entity.MerchantApplyMessage;
import com.lucky.seckill.common.model.entity.Notify;
import com.lucky.seckill.common.mq.MerchantApplyProducer;
import com.lucky.seckill.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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
            log.info("==> 收到商家申请消息, messageId: {}, shopId: {}, userId: {}, shopName: {}",
                    message.getMessageId(),
                    message.getShopId(),
                    message.getUserId(),
                    message.getShopName());

            Notify notify = Notify.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .sendUserId(0L)
                    .receiveUserId(message.getUserId())
                    .title("开店申请提交")
                    .content(String.format("您提交的店铺【%s】开店申请已收到，等待管理员审核", message.getShopName()))
                    .notifyType(1)
                    .businessId(message.getShopId())
                    .subType(0)
                    .isRead(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDelete(0)
                    .build();

            Response<Void> response = notifyFeignClient.saveMerchantApplyNotify(notify);
            if (response == null || !response.isSuccess()) {
                throw new RuntimeException("保存商家申请通知失败");
            }

            log.info("==> 商家申请通知已保存到数据库, messageId: {}, notifyId: {}",
                    message.getMessageId(), notify.getId());
        } catch (Exception e) {
            log.error("==> 处理商家申请消息失败, messageId: {}, error: {}",
                    message.getMessageId(), e.getMessage(), e);
            throw new RuntimeException("处理商家申请消息失败", e);
        }
    }
}
