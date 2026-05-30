package com.lucky.seckill.notify.service;


import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.notify.model.dto.NotifyQueryRequest;
import com.lucky.seckill.notify.model.entity.Notify;

public interface NotifyService {

    /**
     * 保存通知（由调用方传入完整字段，含 id）
     *
     * @return
     */
    Boolean saveNotify(Notify notify);

    /**
     * 保存商户申请通知（MQ 消费场景，自动生成 id 等默认值）
     */
    void saveMerchantApplyNotify(Notify notify);

    /**
     * 将指定消息标记为已读
     *
     * @param notifyId 消息ID
     */
    void markNotifyRead(Long notifyId);

    /**
     * 管理员分页查询指定类型的通知列表
     */
    PageResult<Notify> selectAdminNotifyPage(NotifyQueryRequest request);

    /**
     * 普通用户分页查询自己的指定类型通知列表
     */
    PageResult<Notify> selectUserNotifyPage(NotifyQueryRequest request);
}
