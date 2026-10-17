package com.lucky.quota.activity.service;

import com.lucky.quota.activity.model.dto.QuotaRequest;
import com.lucky.quota.activity.model.vo.QuotaResultVO;
import com.lucky.quota.common.utils.Response;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface QuotaService {

    /**
     * 执行限量申领
     */
    Response<QuotaResultVO> doQuota(QuotaRequest request);

    /**
     * 查询限量申领单异步处理结果（前端轮询用）。
     * <p>
     * 返回值 status：-1=处理中，0=待确认领取（创建成功），-2=创建失败。
     * Redis key 过期（超过30分钟）返回 null → 前端展示"申领单创建超时"。
     */
    Response<QuotaResultVO> getQuotaResult(String orderNo);

    /**
     * SSE 实时推送限量申领单处理结果（替代前端轮询）。
     * <p>
     * 服务端每 500ms 检查 Redis 状态，变更时主动推送，最长 60s 超时。
     */
    SseEmitter streamQuotaResult(String orderNo, Long userId);
}
