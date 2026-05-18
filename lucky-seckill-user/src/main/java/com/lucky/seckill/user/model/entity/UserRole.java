package com.lucky.seckill.user.model.entity;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
* 用户-角色关联表
* @TableName t_user_role
*/
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data

public class UserRole implements Serializable {


    @NotNull(message="[]不能为空")
    private Long id;

    @NotNull(message="[]不能为空")
    private Long userId;

    @NotNull(message="[]不能为空")
    private Long roleId;


    @NotNull(message="[]不能为空")
    private Date createTime;

    /**
     * 逻辑删除
     */
    private Integer isDelete;

}
