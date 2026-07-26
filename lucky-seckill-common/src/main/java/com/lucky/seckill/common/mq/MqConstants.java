package com.lucky.seckill.common.mq;

public class MqConstants {

    private MqConstants() {
    }

    public static final String MERCHANT_APPLY_TOPIC = "merchant-apply-topic";
    public static final String MERCHANT_APPLY_TAG = "merchant-apply";

    public static final String SECKILL_ORDER_TOPIC = "seckill-order-topic";
    public static final String SECKILL_ORDER_TAG = "seckill-order";

    public static final String ORDER_DELIVERY_TOPIC = "order-delivery-topic";
    public static final String ORDER_DELIVERY_TAG = "order-delivery";

    public static final String ORDER_RECEIVE_TOPIC = "order-receive-topic";
    public static final String ORDER_RECEIVE_TAG = "order-receive";

    /** 秒杀订单死信队列 */
    public static final String SECKILL_ORDER_DLQ_TOPIC = "seckill-order-dlq-topic";
    public static final String SECKILL_ORDER_DLQ_TAG = "seckill-order-dlq";

    /** 订单过期检查（延迟队列，替代定时任务扫表） */
    public static final String ORDER_EXPIRE_CHECK_TOPIC = "order-expire-check-topic";
    public static final String ORDER_EXPIRE_CHECK_TAG = "order-expire-check";
}