package com.lucky.seckill.activity;

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
        "com.lucky.seckill.activity",
        "com.lucky.seckill.goods.feign",
        "com.lucky.seckill.notify.feign"
})
@ComponentScan({"com.lucky.seckill.activity", "com.lucky.seckill.common"})
@MapperScan("com.lucky.seckill.activity.mapper")
public class ActivityApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActivityApplication.class, args);
    }
}
