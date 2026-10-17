package com.lucky.quota.goods.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物资审核请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsAuditRequest {

    @NotNull(message = "物资ID不能为空")
    private Long goodsId;

    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String auditRemark;

    /**
     * 审核通过
     */
    public static final Integer AUDIT_PASS = 2;

    /**
     * 审核拒绝
     */
    public static final Integer AUDIT_REJECT = 3;
}
