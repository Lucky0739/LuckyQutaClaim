package com.lucky.seckill.cart.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartVO {

    private Long id;

    private Long goodsId;

    private Long skuId;

    private Long shopId;

    private String shopName;

    private String goodsName;

    private String goodsImg;

    private String skuSpecName;

    private BigDecimal salePrice;

    private Integer stockNum;

    /** 商品上架状态（列表内部校验用） */
    private Integer goodsStatus;

    /** 商品审核状态 */
    private Integer auditStatus;

    /** SKU 状态 */
    private Integer skuStatus;

    private Integer goodsCount;

    private Integer selected;

    /** 小计 = salePrice * goodsCount */
    private BigDecimal subtotal;

    /** 是否可购买 */
    private Boolean valid;

    /** 不可购买原因 */
    private String invalidReason;

    private LocalDateTime createTime;
}
