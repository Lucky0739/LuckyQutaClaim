package com.lucky.seckill.goods.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoodsCategory {
    private Long id;

    private String categoryName;

    private Long parentId;

    private Integer sort;

    private Integer status;

    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}