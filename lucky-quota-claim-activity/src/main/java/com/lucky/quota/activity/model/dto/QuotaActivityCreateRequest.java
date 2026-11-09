package com.lucky.quota.activity.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuotaActivityCreateRequest {

    @NotBlank(message = "场次名称不能为空")
    @Size(max = 100, message = "场次名称不能超过100字")
    private String activityName;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime beginTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Size(max = 500, message = "场次说明不能超过500字")
    private String description;
}
