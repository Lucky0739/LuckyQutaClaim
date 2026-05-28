package com.lucky.seckill.user.model.vo;

import com.lucky.seckill.user.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: Lucky
 * @Date: 2026/5/18
 * @Version: v1.0.0
 * @Description: 管理员用户列表响应VO
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUserListVO {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号（脱敏）
     */
    private String mobile;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 用户状态（0-正常，1-禁用）
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 角色名称列表
     */
    private List<String> roleNames;

    /**
     * 从实体转换为VO
     */
    public static AdminUserListVO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return AdminUserListVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .mobile(desensitizeMobile(user.getMobile()))
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .statusDesc(getStatusDesc(user.getStatus()))
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }

    /**
     * 手机号脱敏
     */
    private static String desensitizeMobile(String mobile) {
        if (mobile == null || mobile.length() < 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    /**
     * 获取状态描述
     */
    private static String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == 0 ? "正常" : "禁用";
    }
}
