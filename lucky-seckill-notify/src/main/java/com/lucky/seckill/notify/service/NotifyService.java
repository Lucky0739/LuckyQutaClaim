package com.lucky.seckill.notify.service;


import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.notify.model.dto.NotifyQueryRequest;
import com.lucky.seckill.notify.model.entity.Notify;
import com.lucky.seckill.notify.model.vo.NotifyListVO;
import com.lucky.seckill.notify.model.vo.NotifyMarkAllReadVO;
import com.lucky.seckill.notify.model.vo.NotifyTypeSummaryVO;
import com.lucky.seckill.notify.model.vo.NotifyUnreadStatVO;
import com.lucky.seckill.notify.model.vo.NotifyVO;

import java.util.List;

public interface NotifyService {

    /**
     * 保存通知（由调用方传入完整字段，含 id）
     */
    Boolean saveNotify(Notify notify);

    /**
     * 保存商户申请通知（MQ 消费场景，自动生成 id 等默认值）
     */
    void saveMerchantApplyNotify(Notify notify);

    /**
     * 将指定消息标记为已读
     */
    void markNotifyRead(Long notifyId);

    /**
     * 按消息类型与业务 ID 标记申请类通知为已读（微服务内部调用）
     */
    void markApplyNotifyRead(Integer notifyType, Long businessId);

    /**
     * 管理员分页查询指定类型的通知列表
     */
    PageResult<Notify> selectAdminNotifyPage(NotifyQueryRequest request);

    /**
     * 当前登录用户分页查询自己收到的消息（receive_user_id = 登录用户ID）
     */
    NotifyListVO selectUserNotifyPage(NotifyQueryRequest request);

    /**
     * 当前登录用户查询消息详情
     */
    NotifyVO getUserNotifyDetail(Long notifyId);

    /**
     * 当前登录用户未读消息总数
     */
    Long countUserUnreadNotify(Integer notifyType);

    /**
     * 按用户 ID、消息类型统计未读消息数量（userId 为空则取当前登录用户）
     */
    List<NotifyUnreadStatVO> listUserUnreadCountByType(Long userId);

    /**
     * 当前登录用户各消息类型 Tab 汇总
     */
    List<NotifyTypeSummaryVO> getUserNotifyTypeSummary();

    /**
     * 当前登录用户消息全部标记为已读（notifyType 为空则标记全部用户可见类型）
     */
    NotifyMarkAllReadVO markAllNotifyRead(Integer notifyType);

    /**
     * 管理员将指定审核类消息全部标记为已读（notifyType 为空则标记全部管理员审核类型）
     */
    NotifyMarkAllReadVO markAllAdminNotifyRead(Integer notifyType);
}
