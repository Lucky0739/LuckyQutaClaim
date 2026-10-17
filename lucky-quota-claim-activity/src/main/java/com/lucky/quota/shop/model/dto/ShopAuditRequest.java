package com.lucky.quota.shop.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发放单位审核请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopAuditRequest {

    @NotNull(message = "发放单位ID不能为空")
    private Long shopId;

    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String auditRemark;

    public static final Integer AUDIT_PASS = 1;
    public static final Integer AUDIT_REJECT = 2;
}
