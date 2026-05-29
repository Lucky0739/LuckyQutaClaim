package com.lucky.seckill.notify.service.impl;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.notify.mapper.NotifyMapper;
import com.lucky.seckill.notify.model.entity.Notify;
import com.lucky.seckill.notify.service.NotifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
    public void markNotifyRead(Long notifyId) {
        if (notifyId == null) {
            log.warn("==> 标记消息为已读失败, notifyId 为空");
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "消息ID不能为空");
        }

        log.info("==> 开始标记消息为已读, notifyId: {}", notifyId);

        Notify notify = notifyMapper.selectByPrimaryKey(notifyId);
        if (Objects.isNull(notify)) {
            log.warn("==> 消息不存在, notifyId: {}", notifyId);
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "消息不存在");
        }

        if (notify.getIsRead() == 1) {
            log.info("==> 消息已是已读状态, notifyId: {}", notifyId);
            return;
        }

        notify.setIsRead(1);
        notify.setUpdateTime(LocalDateTime.now());

        int result = notifyMapper.updateByPrimaryKeySelective(notify);
        if (result == 0) {
            log.error("==> 更新消息状态失败, 影响行数为0, notifyId: {}", notifyId);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "标记已读失败");
        }

        log.info("==> 消息已标记为已读, notifyId: {}", notifyId);
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

    @Override
    public PageResult<Notify> selectByNotifyTypePage(Integer notifyType, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Notify> records = notifyMapper.selectByNotifyTypePage(notifyType, offset, pageSize);
        Long total = notifyMapper.countByNotifyType(notifyType);
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return PageResult.<Notify>builder()
                .records(records)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }
}
