package com.lucky.quota.activity.service;

import com.lucky.quota.activity.model.dto.QuotaGoodsApplyQueryRequest;
import com.lucky.quota.activity.model.dto.QuotaGoodsApplyRequest;
import com.lucky.quota.activity.model.dto.QuotaGoodsAuditRequest;
import com.lucky.quota.activity.model.vo.QuotaGoodsApplyVO;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.common.utils.Response;

public interface QuotaGoodsService {

    Response<Long> applyQuotaGoods(QuotaGoodsApplyRequest request);

    Response<Void> auditQuotaGoods(QuotaGoodsAuditRequest request);

    Response<PageResult<QuotaGoodsApplyVO>> listApplyPage(QuotaGoodsApplyQueryRequest request);

    Response<QuotaGoodsApplyVO> getQuotaGoodsDetail(Long seckillGoodsId);
}
