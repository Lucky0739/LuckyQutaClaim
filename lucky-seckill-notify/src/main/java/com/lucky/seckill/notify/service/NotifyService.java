package com.lucky.seckill.notify.service;


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
     * 将指定业务的开店申请类通知标记为已读
     */
    void markApplyNotifyRead(Integer notifyType, Long businessId);
}
