package com.lucky.quota.goods.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
@TableName("t_goods_img")
public class GoodsImg {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long goodsId;

    private String imgUrl;

    private Integer sort;

    private LocalDateTime createTime;
}