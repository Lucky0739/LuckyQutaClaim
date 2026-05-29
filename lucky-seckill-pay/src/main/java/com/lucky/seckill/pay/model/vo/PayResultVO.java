package com.lucky.seckill.pay.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayResultVO {
    private String orderNo;
    private String payUrl;
    private String qrCode;
    private String tradeNo;
}
