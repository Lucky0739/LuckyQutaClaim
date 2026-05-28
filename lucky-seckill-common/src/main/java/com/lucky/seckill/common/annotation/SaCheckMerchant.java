package com.lucky.seckill.common.annotation;

import java.lang.annotation.*;

/**
 * Merchant permission check annotation
 * Checks if the current user is a merchant with a valid shop
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SaCheckMerchant {
}
