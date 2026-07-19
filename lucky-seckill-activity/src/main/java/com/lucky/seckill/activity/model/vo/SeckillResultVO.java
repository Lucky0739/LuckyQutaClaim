package com.lucky.seckill.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResultVO {

    private String orderNo;

    /** 订单状态：-1=处理中，0=待支付 */
    private Integer status;

    private String message;
}
