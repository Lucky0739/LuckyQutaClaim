package com.lucky.seckill.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户地址响应VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAddressVO {

    /** 地址ID */
    private Long id;

    /** 收货人姓名 */
    private String receiver;

    /** 收货人手机号 */
    private String mobile;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区县 */
    private String area;

    /** 详细地址 */
    private String detailAddress;

    /** 是否默认 0-否 1-是 */
    private Integer isDefault;
}
