-- 秒杀订单最终失败记录表（DLQ 重试全部耗尽后落库，供人工介入处理）
CREATE TABLE IF NOT EXISTS `t_seckill_failed_order`
(
    `id`               BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`          BIGINT(20)   NOT NULL COMMENT '用户ID',
    `activity_id`      BIGINT(20)   NOT NULL COMMENT '秒杀活动ID',
    `goods_id`         BIGINT(20)   NOT NULL COMMENT '商品ID',
    `sku_id`           BIGINT(20)   NOT NULL COMMENT 'SKU ID',
    `seckill_goods_id` BIGINT(20)   NOT NULL COMMENT '秒杀商品ID',
    `order_no`         VARCHAR(64)  NOT NULL COMMENT '订单号',
    `seckill_price`    DECIMAL(10, 2) DEFAULT NULL COMMENT '秒杀价格',
    `goods_name`       VARCHAR(256)   DEFAULT NULL COMMENT '商品名称',
    `goods_img`        VARCHAR(512)   DEFAULT NULL COMMENT '商品图片',
    `buy_num`          INT(11)        DEFAULT 1 COMMENT '购买数量',
    `message_json`     TEXT COMMENT '完整的消息JSON（用于重试时反序列化）',
    `last_error`       VARCHAR(1024)  DEFAULT NULL COMMENT '最后一次失败的错误信息',
    `retry_count`      INT(11)        DEFAULT 0 COMMENT '已重试次数（主队列 + DLQ 总次数）',
    `handle_status`    TINYINT(1)     DEFAULT 0 COMMENT '处理状态：0=待人工处理，1=已手动重试成功，2=已手动放弃',
    `handle_remark`    VARCHAR(512)   DEFAULT NULL COMMENT '处理备注（管理员填写）',
    `handled_by`       VARCHAR(64)    DEFAULT NULL COMMENT '处理人',
    `handled_time`     DATETIME       DEFAULT NULL COMMENT '处理时间',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_handle_status` (`handle_status`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='秒杀订单最终失败记录表（DLQ重试耗尽，人工处理）';
