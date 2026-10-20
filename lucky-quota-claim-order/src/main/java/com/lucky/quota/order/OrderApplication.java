package com.lucky.quota.order;

import com.lucky.quota.common.config.FeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {
        "com.lucky.quota.order.feign",
        "com.lucky.quota.goods.feign",
        "com.lucky.quota.user.feign",
        "com.lucky.quota.shop.feign",
        "com.lucky.quota.notify.feign"
}, defaultConfiguration = FeignConfig.class)
@ComponentScan({
        "com.lucky.quota.order",
        "com.lucky.quota.common"
})
@MapperScan({
        "com.lucky.quota.order.mapper",
        "com.lucky.quota.common.mapper"
})
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
