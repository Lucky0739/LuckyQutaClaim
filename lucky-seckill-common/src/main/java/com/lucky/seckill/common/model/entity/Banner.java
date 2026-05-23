package com.lucky.seckill.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Banner {
    private Long id;
    private String bannerUrl;
    private Integer sort;
    private Integer status;
    private Integer isDelete;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}