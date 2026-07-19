package com.lucky.seckill.activity.service;

import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.common.model.entity.SeckillOrderMessage;
import com.lucky.seckill.common.utils.Response;

/**
 * 秒杀失败订单人工处理服务。
 * <p>
 * 当 DLQ 重试全部耗尽后，失败订单落库，由管理员通过后台手动处理。
 */
public interface SeckillFailedOrderService {

    /**
     * 持久化最终失败的订单（DLQ 重试耗尽后调用）。
     *
     * @param message   原始订单消息
     * @param lastError 最后一次失败的错误信息
     */
    void saveFailedOrder(SeckillOrderMessage message, String lastError);

    /**
     * 分页查询失败订单列表。
     *
     * @param handleStatus 处理状态（null=全部，0=待处理，1=已重试成功，2=已放弃）
     * @param pageNum      页码
     * @param pageSize     每页条数
     */
    Response<PageResult<?>> listFailedOrders(Integer handleStatus, int pageNum, int pageSize);

    /**
     * 手动重试：将失败订单重新投递到主队列。
     *
     * @param id       失败记录 ID
     * @param operator 操作人
     */
    Response<Void> manualRetry(Long id, String operator);

    /**
     * 手动放弃：标记失败订单为已放弃处理。
     *
     * @param id       失败记录 ID
     * @param remark   处理备注
     * @param operator 操作人
     */
    Response<Void> manualDiscard(Long id, String remark, String operator);
}
