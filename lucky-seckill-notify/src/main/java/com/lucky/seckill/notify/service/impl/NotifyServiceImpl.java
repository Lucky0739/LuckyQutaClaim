package com.lucky.seckill.notify.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.notify.mapper.NotifyMapper;
import com.lucky.seckill.notify.model.dto.NotifyQueryRequest;
import com.lucky.seckill.notify.model.entity.Notify;
import com.lucky.seckill.notify.service.NotifyReadRedisService;
import com.lucky.seckill.notify.service.NotifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {

    @Resource
    private NotifyMapper notifyMapper;

    @Resource
    private NotifyReadRedisService notifyReadRedisService;

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

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 开始标记消息为已读, notifyId: {}, userId: {}", notifyId, userId);

        Notify notify = notifyMapper.selectByPrimaryKey(notifyId);
        if (Objects.isNull(notify)) {
            log.warn("==> 消息不存在, notifyId: {}", notifyId);
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "消息不存在");
        }

        if (!userId.equals(notify.getReceiveUserId())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "无权操作该消息");
        }

        if (notify.getIsRead() == null || notify.getIsRead() != 1) {
            notify.setIsRead(1);
            notify.setReadTime(LocalDateTime.now());
            notify.setUpdateTime(LocalDateTime.now());

            int result = notifyMapper.updateByPrimaryKeySelective(notify);
            if (result == 0) {
                log.error("==> 更新消息状态失败, 影响行数为0, notifyId: {}", notifyId);
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "标记已读失败");
            }
        }

        notifyReadRedisService.addReadNotify(userId, notify.getNotifyType(), notifyId);
        log.info("==> 消息已标记为已读并写入Redis ZSet, notifyId: {}, notifyType: {}", notifyId, notify.getNotifyType());
    }

    @Override
    public PageResult<Notify> selectAdminNotifyPage(NotifyQueryRequest request) {
        validateNotifyQueryRequest(request);
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        Long adminId = StpUtil.getLoginIdAsLong();
        List<Notify> records = notifyMapper.selectByNotifyTypePage(request.getNotifyType(), offset, pageSize);
        Long total = notifyMapper.countByNotifyType(request.getNotifyType());
        enrichReadStatus(adminId, request.getNotifyType(), records);

        return buildPageResult(records, total, pageNum, pageSize);
    }

    @Override
    public PageResult<Notify> selectUserNotifyPage(NotifyQueryRequest request) {
        validateNotifyQueryRequest(request);
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        Long userId = StpUtil.getLoginIdAsLong();
        List<Notify> records = notifyMapper.selectByReceiveUserIdAndNotifyTypePage(
                userId, request.getNotifyType(), offset, pageSize);
        Long total = notifyMapper.countByReceiveUserIdAndNotifyType(userId, request.getNotifyType());
        enrichReadStatus(userId, request.getNotifyType(), records);

        return buildPageResult(records, total, pageNum, pageSize);
    }

    private void validateNotifyQueryRequest(NotifyQueryRequest request) {
        if (request == null || request.getNotifyType() == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "消息类型不能为空");
        }
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
    }

    /**
     * 合并数据库与 Redis ZSet 中的已读状态
     */
    private void enrichReadStatus(Long userId, Integer notifyType, List<Notify> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> readIds = notifyReadRedisService.getReadNotifyIds(userId, notifyType);
        for (Notify notify : records) {
            boolean readInRedis = readIds.contains(notify.getId());
            boolean readInDb = notify.getIsRead() != null && notify.getIsRead() == 1;
            if (readInRedis || readInDb) {
                notify.setIsRead(1);
            }
        }
    }

    private PageResult<Notify> buildPageResult(List<Notify> records, Long total, int pageNum, int pageSize) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return PageResult.<Notify>builder()
                .records(records)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
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
