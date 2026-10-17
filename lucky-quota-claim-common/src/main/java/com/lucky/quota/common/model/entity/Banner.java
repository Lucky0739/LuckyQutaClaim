package com.lucky.quota.common.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_banner")
public class Banner {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 背景图 URL
     */
    @TableField("bannerUrl")
    private String bannerUrl;
    private Integer sort;
    @TableField(exist = false)
    private Integer status;
    @TableLogic
    private Integer isDelete;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}