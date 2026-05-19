package com.lucky.seckill.shop.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 开店请求DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateShopRequest {

    /**
     * 店铺名称
     */
    @NotBlank(message = "店铺名称不能为空")
    private String shopName;

    /**
     * 店铺头像/LOGO
     */
    private String shopAvatar;

    /**
     * 店铺简介
     */
    private String shopDesc;
}
