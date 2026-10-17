package com.lucky.quota.activity.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucky.quota.activity.mapper.QuotaFailedOrderMapper;
import com.lucky.quota.activity.model.entity.QuotaFailedOrder;
import com.lucky.quota.activity.model.vo.QuotaFailedOrderVO;
import com.lucky.quota.activity.mq.QuotaOrderProducer;
import com.lucky.quota.activity.service.QuotaFailedOrderService;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.model.entity.QuotaOrderMessage;
import com.lucky.quota.common.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 限量申领失败申领单人工处理服务实现。
 */
@Service
@Slf4j
public class QuotaFailedOrderServiceImpl implements QuotaFailedOrderService {

    /** 处理状态常量 */
    private static final int HANDLE_STATUS_PENDING = 0;   // 待人工处理
    private static final int HANDLE_STATUS_RETRIED = 1;   // 已手动重试成功
    private static final int HANDLE_STATUS_DISCARDED = 2; // 已手动放弃

    @Resource
    private QuotaFailedOrderMapper seckillFailedOrderMapper;

    @Resource
    private QuotaOrderProducer seckillOrderProducer;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void saveFailedOrder(QuotaOrderMessage message, String lastError) {
        try {
            // 幂等检查：同一 orderNo 只记录一次
            QuotaFailedOrder exist = seckillFailedOrderMapper.selectByOrderNo(message.getOrderNo());
            if (exist != null) {
                log.warn("==> 【失败申领单】orderNo={} 已存在失败记录，跳过重复落库", message.getOrderNo());
                return;
            }

            String messageJson;
            try {
                messageJson = objectMapper.writeValueAsString(message);
            } catch (JsonProcessingException e) {
                log.error("==> 【失败申领单】序列化消息失败, orderNo={}", message.getOrderNo(), e);
                messageJson = "序列化失败: " + e.getMessage();
            }

            LocalDateTime now = LocalDateTime.now();
            QuotaFailedOrder record = QuotaFailedOrder.builder()
                    .userId(message.getUserId())
                    .activityId(message.getActivityId())
                    .goodsId(message.getGoodsId())
                    .skuId(message.getSkuId())
                    .seckillGoodsId(message.getSeckillGoodsId())
                    .orderNo(message.getOrderNo())
                    .seckillPrice(message.getSeckillPrice())
                    .goodsName(message.getGoodsName())
                    .goodsImg(message.getGoodsImg())
                    .buyNum(message.getBuyNum())
                    .messageJson(messageJson)
                    .lastError(lastError)
                    .retryCount(message.getRetryCount() == null ? 0 : message.getRetryCount())
                    .handleStatus(HANDLE_STATUS_PENDING)
                    .createTime(now)
                    .updateTime(now)
                    .build();

            seckillFailedOrderMapper.insertSelective(record);
            log.info("==> 【失败申领单】已落库等待人工处理, orderNo={}, id={}", message.getOrderNo(), record.getId());

        } catch (Exception e) {
            log.error("==> 【失败申领单】落库失败, orderNo={}", message.getOrderNo(), e);
        }
    }

    @Override
    public Response<PageResult<?>> listFailedOrders(Integer handleStatus, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<QuotaFailedOrder> records = seckillFailedOrderMapper.selectPendingPage(handleStatus, offset, pageSize);
        long total = seckillFailedOrderMapper.countPending(handleStatus);

        List<QuotaFailedOrderVO> vos = records.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        PageResult<QuotaFailedOrderVO> pageResult = PageResult.<QuotaFailedOrderVO>builder()
                .records(vos)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();

        return Response.success(pageResult);
    }

    @Override
    public Response<Void> manualRetry(Long id, String operator) {
        QuotaFailedOrder record = seckillFailedOrderMapper.selectByPrimaryKey(id);
        if (record == null) {
            return Response.fail("失败申领单记录不存在");
        }
        if (record.getHandleStatus() != HANDLE_STATUS_PENDING) {
            return Response.fail("该申领单已处理，状态=" + toStatusText(record.getHandleStatus()));
        }

        // 从 messageJson 反序列化原始消息，重新投递到主队列
        QuotaOrderMessage message;
        try {
            message = objectMapper.readValue(record.getMessageJson(), QuotaOrderMessage.class);
        } catch (Exception e) {
            log.error("==> 【失败申领单】反序列化消息失败, id={}, orderNo={}", id, record.getOrderNo(), e);
            return Response.fail("消息数据损坏，无法重试");
        }

        // 重置重试次数，从头开始
        message.setRetryCount(0);

        try {
            seckillOrderProducer.sendQuotaOrderMessage(message);
            log.info("==> 【失败申领单】手动重试已投递到主队列, id={}, orderNo={}, operator={}",
                    id, record.getOrderNo(), operator);
        } catch (Exception e) {
            log.error("==> 【失败申领单】手动重试投递失败, id={}, orderNo={}", id, record.getOrderNo(), e);
            return Response.fail("消息投递失败: " + e.getMessage());
        }

        // 标记为已重试
        seckillFailedOrderMapper.updateHandleStatus(
                id, HANDLE_STATUS_RETRIED, "手动重试, operator=" + operator, operator);

        return Response.success(null);
    }

    @Override
    public Response<Void> manualDiscard(Long id, String remark, String operator) {
        QuotaFailedOrder record = seckillFailedOrderMapper.selectByPrimaryKey(id);
        if (record == null) {
            return Response.fail("失败申领单记录不存在");
        }
        if (record.getHandleStatus() != HANDLE_STATUS_PENDING) {
            return Response.fail("该申领单已处理，状态=" + toStatusText(record.getHandleStatus()));
        }

        seckillFailedOrderMapper.updateHandleStatus(
                id, HANDLE_STATUS_DISCARDED, remark, operator);

        log.info("==> 【失败申领单】已手动放弃, id={}, orderNo={}, operator={}, remark={}",
                id, record.getOrderNo(), operator, remark);

        return Response.success(null);
    }

    private QuotaFailedOrderVO toVO(QuotaFailedOrder record) {
        return QuotaFailedOrderVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .activityId(record.getActivityId())
                .goodsId(record.getGoodsId())
                .skuId(record.getSkuId())
                .seckillGoodsId(record.getSeckillGoodsId())
                .orderNo(record.getOrderNo())
                .seckillPrice(record.getSeckillPrice())
                .goodsName(record.getGoodsName())
                .goodsImg(record.getGoodsImg())
                .buyNum(record.getBuyNum())
                .lastError(record.getLastError())
                .retryCount(record.getRetryCount())
                .handleStatus(record.getHandleStatus())
                .handleStatusText(toStatusText(record.getHandleStatus()))
                .handleRemark(record.getHandleRemark())
                .handledBy(record.getHandledBy())
                .handledTime(record.getHandledTime())
                .createTime(record.getCreateTime())
                .build();
    }

    private String toStatusText(Integer handleStatus) {
        if (handleStatus == null) {
            return "未知";
        }
        return switch (handleStatus) {
            case HANDLE_STATUS_PENDING -> "待人工处理";
            case HANDLE_STATUS_RETRIED -> "已手动重试";
            case HANDLE_STATUS_DISCARDED -> "已手动放弃";
            default -> "未知(" + handleStatus + ")";
        };
    }
}
