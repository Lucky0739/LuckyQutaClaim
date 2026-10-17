package com.lucky.quota.shop.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 申请发放单位请求 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateShopRequest {

    /**
     * 发放单位名称
     */
    @NotBlank(message = "发放单位名称不能为空")
    private String shopName;

    /**
     * 单位头像/LOGO
     */
    private String shopAvatar;

    /**
     * 单位简介
     */
    private String shopDesc;
}
