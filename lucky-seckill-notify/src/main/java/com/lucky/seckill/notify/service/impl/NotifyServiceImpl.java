package com.lucky.seckill.notify.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.model.entity.PageResult;
import com.lucky.seckill.notify.enums.NotifyTypeEnum;
import com.lucky.seckill.notify.mapper.NotifyMapper;
import com.lucky.seckill.notify.model.dto.NotifyQueryRequest;
import com.lucky.seckill.notify.model.dto.NotifyTypeCountDTO;
import com.lucky.seckill.notify.model.entity.Notify;
import com.lucky.seckill.notify.model.vo.NotifyListVO;
import com.lucky.seckill.notify.model.vo.NotifyMarkAllReadVO;
import com.lucky.seckill.notify.model.vo.NotifyTypeSummaryVO;
import com.lucky.seckill.notify.model.vo.NotifyUnreadStatVO;
import com.lucky.seckill.notify.model.vo.NotifyVO;
import com.lucky.seckill.notify.service.NotifyReadRedisService;
import com.lucky.seckill.notify.service.NotifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "消息ID不能为空");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 开始标记消息为已读, notifyId: {}, userId: {}", notifyId, userId);

        Notify notify = notifyMapper.selectByPrimaryKey(notifyId);
        if (Objects.isNull(notify)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "消息不存在");
        }

        if (!userId.equals(notify.getReceiveUserId())) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED, "无权操作该消息");
        }

        if (notify.getIsRead() == null || notify.getIsRead() != 1) {
            notify.setIsRead(1);
            notify.setReadTime(LocalDateTime.now());
            notify.setUpdateTime(LocalDateTime.now());

            int result = notifyMapper.updateByPrimaryKeySelective(notify);
            if (result == 0) {
                throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "标记已读失败");
            }
        }

        notifyReadRedisService.addReadNotify(userId, notify.getNotifyType(), notifyId);
        log.info("==> 消息已标记为已读, notifyId: {}, notifyType: {}", notifyId, notify.getNotifyType());
    }

    @Override
    public void markApplyNotifyRead(Integer notifyType, Long businessId) {
        if (notifyType == null || businessId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "消息类型或业务ID不能为空");
        }

        Notify notify = notifyMapper.selectByNotifyTypeAndBusinessId(notifyType, businessId);
        if (Objects.isNull(notify)) {
            log.warn("==> 未找到申请通知, notifyType: {}, businessId: {}", notifyType, businessId);
            return;
        }

        if (notify.getIsRead() != null && notify.getIsRead() == 1) {
            return;
        }

        notify.setIsRead(1);
        notify.setReadTime(LocalDateTime.now());
        notify.setUpdateTime(LocalDateTime.now());

        int result = notifyMapper.updateByPrimaryKeySelective(notify);
        if (result == 0) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "标记已读失败");
        }

        Long redisUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : notify.getReceiveUserId();
        notifyReadRedisService.addReadNotify(redisUserId, notifyType, notify.getId());
        if (notify.getReceiveUserId() != null && !notify.getReceiveUserId().equals(redisUserId)) {
            notifyReadRedisService.addReadNotify(notify.getReceiveUserId(), notifyType, notify.getId());
        }
        log.info("==> 申请通知已标记为已读, notifyId: {}, notifyType: {}, businessId: {}",
                notify.getId(), notifyType, businessId);
    }

    @Override
    public PageResult<Notify> selectAdminNotifyPage(NotifyQueryRequest request) {
        validateAdminNotifyQueryRequest(request);
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
    public NotifyListVO selectUserNotifyPage(NotifyQueryRequest request) {
        validateUserNotifyQueryRequest(request);
        Long userId = getCurrentLoginUserId();

        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        List<Notify> records = notifyMapper.selectUserNotifyPage(
                userId, request.getNotifyType(), request.getIsRead(), offset, pageSize);
        enrichUserReadStatus(userId, records);

        Long total = notifyMapper.countUserNotify(userId, request.getNotifyType(), request.getIsRead());
        Long unreadCount = notifyMapper.countUserUnreadNotify(userId, request.getNotifyType());
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);

        List<NotifyVO> voList = records.stream()
                .map(NotifyVO::from)
                .collect(Collectors.toList());

        return NotifyListVO.builder()
                .records(voList)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .unreadCount(unreadCount)
                .build();
    }

    @Override
    public NotifyVO getUserNotifyDetail(Long notifyId) {
        if (notifyId == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "消息ID不能为空");
        }
        Long userId = getCurrentLoginUserId();

        Notify notify = notifyMapper.selectByPrimaryKey(notifyId);
        if (notify == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "消息不存在");
        }
        if (!userId.equals(notify.getReceiveUserId())) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED, "无权查看该消息");
        }

        enrichUserReadStatus(userId, List.of(notify));
        return NotifyVO.from(notify);
    }

    @Override
    public Long countUserUnreadNotify(Integer notifyType) {
        Long userId = getCurrentLoginUserId();
        return notifyMapper.countUserUnreadNotify(userId, notifyType);
    }

    @Override
    public List<NotifyUnreadStatVO> listUserUnreadCountByType(Long userId) {
        Long targetUserId = resolveTargetUserId(userId);
        List<NotifyUnreadStatVO> statList = notifyMapper.countUnreadGroupByUserIdAndNotifyType(targetUserId);
        Map<Integer, Long> unreadMap = statList.stream()
                .collect(Collectors.toMap(NotifyUnreadStatVO::getNotifyType, NotifyUnreadStatVO::getUnreadCount));

        List<NotifyUnreadStatVO> result = new ArrayList<>();
        for (NotifyTypeEnum typeEnum : NotifyTypeEnum.userVisibleTypes()) {
            result.add(NotifyUnreadStatVO.builder()
                    .receiveUserId(targetUserId)
                    .notifyType(typeEnum.getCode())
                    .notifyTypeName(typeEnum.getName())
                    .unreadCount(unreadMap.getOrDefault(typeEnum.getCode(), 0L))
                    .build());
        }
        return result;
    }

    @Override
    public List<NotifyTypeSummaryVO> getUserNotifyTypeSummary() {
        Long userId = getCurrentLoginUserId();
        List<NotifyTypeCountDTO> countList = notifyMapper.countUserNotifySummaryGroupByType(userId);
        Map<Integer, NotifyTypeCountDTO> countMap = countList.stream()
                .collect(Collectors.toMap(NotifyTypeCountDTO::getNotifyType, Function.identity()));

        List<NotifyTypeSummaryVO> summaryList = new ArrayList<>();
        for (NotifyTypeEnum typeEnum : NotifyTypeEnum.userVisibleTypes()) {
            NotifyTypeCountDTO countDTO = countMap.get(typeEnum.getCode());
            long totalCount = countDTO == null || countDTO.getTotalCount() == null ? 0L : countDTO.getTotalCount();
            long unreadCount = countDTO == null || countDTO.getUnreadCount() == null ? 0L : countDTO.getUnreadCount();
            summaryList.add(NotifyTypeSummaryVO.builder()
                    .notifyType(typeEnum.getCode())
                    .notifyTypeName(typeEnum.getName())
                    .totalCount(totalCount)
                    .unreadCount(unreadCount)
                    .build());
        }
        return summaryList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotifyMarkAllReadVO markAllNotifyRead(Integer notifyType) {
        if (notifyType != null && !NotifyTypeEnum.isUserVisibleType(notifyType)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "不支持将该类型消息全部标记已读");
        }

        Long userId = getCurrentLoginUserId();
        List<Notify> unreadList = notifyMapper.selectUserUnreadNotifyList(userId, notifyType);
        if (unreadList.isEmpty()) {
            log.info("==> 用户无未读消息需标记, userId: {}, notifyType: {}", userId, notifyType);
            return NotifyMarkAllReadVO.builder().markedCount(0).build();
        }

        LocalDateTime now = LocalDateTime.now();
        int rows = notifyMapper.markAllReadByReceiveUserId(userId, notifyType, now, now);
        syncReadNotifyToRedis(userId, unreadList);

        log.info("==> 用户消息全部标记已读, userId: {}, notifyType: {}, count: {}", userId, notifyType, rows);
        return NotifyMarkAllReadVO.builder().markedCount(rows).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotifyMarkAllReadVO markAllAdminNotifyRead(Integer notifyType) {
        if (!StpUtil.hasRole("admin")) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED, "仅管理员可操作");
        }
        if (notifyType != null && !NotifyTypeEnum.isAdminApplyType(notifyType)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "仅支持审核类消息全部标记已读");
        }

        Long adminId = getCurrentLoginUserId();
        int totalMarked = 0;

        if (notifyType != null) {
            totalMarked = markAdminNotifyTypeRead(adminId, notifyType);
        } else {
            for (NotifyTypeEnum typeEnum : NotifyTypeEnum.adminApplyTypes()) {
                totalMarked += markAdminNotifyTypeRead(adminId, typeEnum.getCode());
            }
        }

        log.info("==> 管理员消息全部标记已读, adminId: {}, notifyType: {}, count: {}",
                adminId, notifyType, totalMarked);
        return NotifyMarkAllReadVO.builder().markedCount(totalMarked).build();
    }

    private int markAdminNotifyTypeRead(Long adminId, Integer notifyType) {
        List<Notify> unreadList = notifyMapper.selectUnreadByNotifyType(notifyType);
        if (unreadList.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = notifyMapper.markAllReadByNotifyType(notifyType, now, now);
        syncReadNotifyToRedis(adminId, unreadList);
        return rows;
    }

    private void syncReadNotifyToRedis(Long userId, List<Notify> notifyList) {
        if (notifyList == null || notifyList.isEmpty()) {
            return;
        }
        Map<Integer, List<Long>> idsByType = notifyList.stream()
                .filter(notify -> notify.getId() != null && notify.getNotifyType() != null)
                .collect(Collectors.groupingBy(
                        Notify::getNotifyType,
                        Collectors.mapping(Notify::getId, Collectors.toList())));
        notifyReadRedisService.batchAddReadNotify(userId, idsByType);
    }

    private void validateAdminNotifyQueryRequest(NotifyQueryRequest request) {
        if (request == null || request.getNotifyType() == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "消息类型不能为空");
        }
        normalizePageParams(request);
    }

    private void validateUserNotifyQueryRequest(NotifyQueryRequest request) {
        if (request == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "查询参数不能为空");
        }
        if (request.getIsRead() != null && request.getIsRead() != 0 && request.getIsRead() != 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "已读状态参数无效");
        }
        normalizePageParams(request);
    }

    private void normalizePageParams(NotifyQueryRequest request) {
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
        if (request.getPageSize() > 100) {
            request.setPageSize(100);
        }
    }

    private Long getCurrentLoginUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private Long resolveTargetUserId(Long userId) {
        Long loginUserId = getCurrentLoginUserId();
        if (userId == null) {
            return loginUserId;
        }
        if (!loginUserId.equals(userId) && !StpUtil.hasRole("admin")) {
            throw new BizException(ResponseCodeEnum.USER_PERMISSION_DENIED, "无权查询其他用户消息统计");
        }
        return userId;
    }

    private void enrichReadStatus(Long userId, Integer notifyType, List<Notify> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> readIds = notifyReadRedisService.getReadNotifyIds(userId, notifyType);
        for (Notify notify : records) {
            mergeReadStatus(notify, readIds.contains(notify.getId()));
        }
    }

    private void enrichUserReadStatus(Long userId, List<Notify> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (Notify notify : records) {
            Set<Long> readIds = notifyReadRedisService.getReadNotifyIds(userId, notify.getNotifyType());
            mergeReadStatus(notify, readIds.contains(notify.getId()));
        }
    }

    private void mergeReadStatus(Notify notify, boolean readInRedis) {
        boolean readInDb = notify.getIsRead() != null && notify.getIsRead() == 1;
        if (readInRedis || readInDb) {
            notify.setIsRead(1);
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
