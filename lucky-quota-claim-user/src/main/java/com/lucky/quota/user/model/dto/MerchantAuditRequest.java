package com.lucky.quota.user.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商家申请审批请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAuditRequest {

    @NotNull(message = "发放单位ID不能为空")
    private Long shopId;

    @NotNull(message = "审批结果不能为空")
    private Integer auditStatus;

    private String auditRemark;

    public static final Integer AUDIT_PASS = 1;
    public static final Integer AUDIT_REJECT = 2;
}
