package com.lucky.seckill.user.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAddress {
    /** 主键ID */
    private Long id;

    /** 用户ID */
    private Long userId;

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

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0-未删除 1-已删除 */
    private Integer isDelete;

}
