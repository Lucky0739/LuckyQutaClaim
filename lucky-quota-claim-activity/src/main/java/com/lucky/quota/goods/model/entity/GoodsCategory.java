package com.lucky.quota.goods.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_goods_category")
public class GoodsCategory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String categoryName;

    private Long parentId;

    private Integer sort;

    private Integer status;

    @TableLogic
    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}