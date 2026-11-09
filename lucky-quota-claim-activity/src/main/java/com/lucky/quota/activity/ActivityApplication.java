package com.lucky.quota.activity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients(basePackages = {
        "com.lucky.quota.activity",
        "com.lucky.quota.activity.feign",
        "com.lucky.quota.goods.feign",
        "com.lucky.quota.shop.feign",
        "com.lucky.quota.notify.feign",
        "com.lucky.quota.user.feign"
})
@ComponentScan({
        "com.lucky.quota.activity",
        "com.lucky.quota.goods",
        "com.lucky.quota.shop",
        "com.lucky.quota.common"
})
@MapperScan({
        "com.lucky.quota.activity.mapper",
        "com.lucky.quota.goods.mapper",
        "com.lucky.quota.shop.mapper",
        "com.lucky.quota.common.mapper"
})
public class ActivityApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActivityApplication.class, args);
    }
}
