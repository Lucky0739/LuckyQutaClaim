package com.lucky.quota.activity.mq;

import com.lucky.quota.common.mq.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 限量申领消费的手工注册配置。
 * <p>
 * <b>为什么不用 {@code @RocketMQMessageListener} 注解</b>：
 * 那个注解是 {@code rocketmq-spring-boot-starter} 的，它要求被标注的 Bean 实现
 * Spring 的 {@link org.apache.rocketmq.spring.core.RocketMQListener}，
 * 否则 {@code RocketMQMessageListenerBeanPostProcessor} 注册容器时会直接抛
 * {@code IllegalStateException: ... is not instance of RocketMQListener}，应用启动失败。
 * <p>
 * 而限量申领的两个消费者要的是**原生**的 {@code MessageListenerConcurrently}：它们需要拿到
 * {@code MessageExt}（取 msgId、reconsumeTimes）并逐条手动裁决 ACK / 升级 DLQ，
 * 而不是让 starter 统一批量重投——那正是「半批成功半批重投」问题的来源。
 * <p>
 * 所以这里手工 new {@code DefaultMQPushConsumer}，把原生监听器注册进去，
 * 既避开 starter 的强校验，又完整保留手动 ACK 的设计。
 * 注意不能用 {@code @RocketMQMessageListener} 的注解写法与手工注册混用，
 * 同一个 consumerGroup 注册两次会报错。
 */
@Configuration
@Slf4j
public class QuotaMqConsumerConfig {

    /** 与各模块 application.yml 里的 rocketmq.name-server 保持一致 */
    @Value("${rocketmq.name-server:127.0.0.1:9876}")
    private String nameServer;

    /**
     * 限量申领主消费者。
     * <p>
     * bean 名刻意不用 {@code seckillOrderConsumer}，避免与 {@link QuotaOrderConsumer}
     * 这个组件自身的 bean 名冲突。
     */
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public DefaultMQPushConsumer seckillOrderPushConsumer(QuotaOrderConsumer listener) throws MQClientException {
        return buildConsumer(
                "seckill-order-consumer-group",
                MqConstants.QUOTA_ORDER_TOPIC,
                MqConstants.QUOTA_ORDER_TAG,
                listener);
    }

    /**
     * 限量申领单死信消费者（延迟退避重试）。
     */
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public DefaultMQPushConsumer seckillOrderDlqPushConsumer(QuotaOrderDlqConsumer listener) throws MQClientException {
        return buildConsumer(
                "seckill-order-dlq-consumer-group",
                MqConstants.QUOTA_ORDER_DLQ_TOPIC,
                MqConstants.QUOTA_ORDER_DLQ_TAG,
                listener);
    }

    private DefaultMQPushConsumer buildConsumer(String group, String topic, String tag,
                                                MessageListenerConcurrently listener) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(group);
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe(topic, tag);
        consumer.registerMessageListener(listener);

        // 逐条消费：手动 ACK 的裁决粒度就是单条消息，批量没有意义。
        // starter 注解的默认值也是 1，这里显式写出来表明是刻意为之。
        consumer.setConsumeMessageBatchMaxSize(1);

        log.info("==> 注册限量申领消费者, group={}, topic={}, tag={}, nameServer={}",
                group, topic, tag, nameServer);
        return consumer;
    }
}
