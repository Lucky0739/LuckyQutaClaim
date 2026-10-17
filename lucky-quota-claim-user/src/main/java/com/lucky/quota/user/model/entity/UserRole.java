package com.lucky.quota.user.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

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

@TableName("t_user_role")
public class UserRole implements Serializable {


    @NotNull(message="[]不能为空")
    @TableId(type = IdType.AUTO)
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
    @TableLogic
    private Integer isDelete;

}
