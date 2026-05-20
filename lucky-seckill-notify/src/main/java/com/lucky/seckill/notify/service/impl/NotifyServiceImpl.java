package com.lucky.seckill.notify.service.impl;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.notify.mapper.NotifyMapper;
import com.lucky.seckill.notify.model.entity.Notify;
import com.lucky.seckill.notify.service.NotifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {

    @Resource
    private NotifyMapper notifyMapper;

    @Override
    public Boolean saveNotify(Notify notify) {
        fillDefaultFields(notify, notify.getId() == null);
        int result = notifyMapper.insertSelective(notify);
        if (result <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "通知保存失败");
        }
        log.info("==> 保存通知成功, id: {}, notifyType: {}, businessId: {}",
                notify.getId(), notify.getNotifyType(), notify.getBusinessId());
        return true;
    }

    @Override
    public void saveMerchantApplyNotify(Notify notify) {
        fillDefaultFields(notify, true);
        int result = notifyMapper.insertSelective(notify);
        if (result <= 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "保存通知失败");
        }
        log.info("==> 保存开店申请通知成功, id: {}, receiveUserId: {}",
                notify.getId(), notify.getReceiveUserId());
    }

    @Override
    public void markApplyNotifyRead(Integer notifyType, Long businessId) {
        Notify notify = notifyMapper.selectUnreadByNotifyTypeAndBusinessId(notifyType, businessId);
        if (Objects.isNull(notify)) {
            log.info("==> 未找到待标记已读的通知, notifyType: {}, businessId: {}", notifyType, businessId);
            return;
        }
        notify.setIsRead(1);
        notify.setUpdateTime(LocalDateTime.now());
        notifyMapper.updateByPrimaryKeySelective(notify);
        log.info("==> 通知已标记为已读, notifyId: {}, businessId: {}", notify.getId(), businessId);
    }

    private void fillDefaultFields(Notify notify, boolean generateId) {
        if (generateId) {
            notify.setId(IdUtil.getSnowflakeNextId());
        }
        if (notify.getIsRead() == null) {
            notify.setIsRead(0);
        }
        if (notify.getIsDelete() == null) {
            notify.setIsDelete(0);
        }
        if (notify.getSubType() == null) {
            notify.setSubType(0);
        }
        if (notify.getCreateTime() == null) {
            notify.setCreateTime(LocalDateTime.now());
        }
        if (notify.getUpdateTime() == null) {
            notify.setUpdateTime(LocalDateTime.now());
        }
    }
}
