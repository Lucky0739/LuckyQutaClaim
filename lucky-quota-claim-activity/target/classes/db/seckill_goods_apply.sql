-- =============================================================================
-- 旧表升级：在原有 t_seckill_goods 上追加申请/审核相关字段
-- 若已执行过或列已存在，跳过对应 ALTER 即可
-- 完整建表见同目录 t_seckill_goods.sql
-- =============================================================================

USE seckill_mall;

ALTER TABLE t_seckill_goods
    ADD COLUMN sku_spec_name VARCHAR(255) NULL COMMENT '绑定的规格名称' AFTER sku_id;

ALTER TABLE t_seckill_goods
    ADD COLUMN audit_status TINYINT NOT NULL DEFAULT 2 COMMENT '审核状态 1待审2通过3拒绝' AFTER sort;

ALTER TABLE t_seckill_goods
    ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT '审核备注' AFTER audit_status;

-- 历史数据默认已审核通过（接口 /activity/{id}/goods 查询 audit_status = 2）
UPDATE t_seckill_goods SET audit_status = 2 WHERE audit_status = 0;

-- 秒杀扣库存、按商品+规格查询时可用的辅助索引（已存在则跳过）
CREATE INDEX idx_goods_sku ON t_seckill_goods (goods_id, sku_id);
