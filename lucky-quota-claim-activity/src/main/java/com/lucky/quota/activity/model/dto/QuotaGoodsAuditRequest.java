package com.lucky.quota.activity.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaGoodsAuditRequest {

    @NotNull(message = "限量物资ID不能为空")
    private Long seckillGoodsId;

    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String auditRemark;

    public static final Integer AUDIT_PASS = 2;

    public static final Integer AUDIT_REJECT = 3;
}
