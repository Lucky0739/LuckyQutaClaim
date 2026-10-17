package com.lucky.quota.shop.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopCollectVO {

    private Long id;

    private Long shopId;

    private String shopName;

    private String shopAvatar;

    private String shopDesc;

    private Integer collectNum;

    private LocalDateTime createTime;
}
