package com.lucky.seckill.shop.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopCollectStatusVO {

    private Long shopId;

    private Boolean collected;

    private Integer collectNum;
}
