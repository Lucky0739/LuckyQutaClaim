package com.lucky.quota.user.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_address")
public class UserAddress {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 领取人姓名 */
    private String receiver;

    /** 领取人手机号 */
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /** 逻辑删除 0-未删除 1-已删除 */
    @TableLogic
    private Integer isDelete;

}