package com.lucky.quota.common.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BannerRequest {
    /**
     * 背景图 URL
     */
    private String bannerUrl;

    /**
     * 排序值
     */
    private Integer sort;
}