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
public class GoodsImg {
    private Long id;

    private Long goodsId;

    private String imgUrl;

    private Integer sort;

    private LocalDateTime createTime;
}