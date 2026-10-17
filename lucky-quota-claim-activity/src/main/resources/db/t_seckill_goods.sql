-- =============================================================================
-- 秒杀商品关联表 t_seckill_goods（标准结构，与 SeckillGoodsMapper 一致）
-- =============================================================================

USE seckill_mall;

-- 新库建表；若表已存在请勿直接执行本文件，改用 seckill_goods_apply.sql 增量升级
CREATE TABLE IF NOT EXISTS t_seckill_goods
(
    id            BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    activity_id   BIGINT UNSIGNED                           NOT NULL COMMENT '活动ID',
    goods_id      BIGINT UNSIGNED                           NOT NULL COMMENT '商品ID',
    sku_id        BIGINT UNSIGNED DEFAULT 0                 NOT NULL COMMENT '秒杀SKU ID',
    sku_spec_name VARCHAR(255)                              NULL COMMENT '绑定的规格名称',
    seckill_title VARCHAR(100)                              NOT NULL COMMENT '秒杀展示名称',
    seckill_img   VARCHAR(255)                              NOT NULL COMMENT '秒杀展示图片',
    seckill_price DECIMAL(10, 2)                            NOT NULL COMMENT '秒杀价',
    seckill_total INT             DEFAULT 0                 NOT NULL COMMENT '秒杀总库存',
    seckill_stock INT             DEFAULT 0                 NOT NULL COMMENT '秒杀剩余库存',
    seckill_limit INT             DEFAULT 1                 NOT NULL COMMENT '每人限购数量',
    sort          INT             DEFAULT 0                 NOT NULL COMMENT '排序',
    audit_status  TINYINT         DEFAULT 2                 NOT NULL COMMENT '审核状态：1待审核 2审核通过 3审核拒绝',
    audit_remark  VARCHAR(500)                              NULL COMMENT '审核备注',
    version       INT UNSIGNED    DEFAULT 0                 NOT NULL COMMENT '乐观锁版本号（防超卖）',
    is_deleted    TINYINT         DEFAULT 0                 NOT NULL COMMENT '是否删除',
    create_time   DATETIME        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time   DATETIME        DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_activity_goods_sku UNIQUE (activity_id, goods_id, sku_id)
) COMMENT '秒杀商品关联表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_activity_sort ON t_seckill_goods (activity_id, sort);

CREATE INDEX idx_goods_sku ON t_seckill_goods (goods_id, sku_id);
