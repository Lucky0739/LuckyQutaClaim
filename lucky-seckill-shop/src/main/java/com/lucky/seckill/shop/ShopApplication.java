package com.lucky.seckill.shop;


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
@EnableFeignClients(basePackages = {"com.lucky.seckill.shop", "com.lucky.seckill.notify", "com.lucky.seckill.user", "com.lucky.seckill.goods"})
@MapperScan("com.lucky.seckill.shop.mapper")
@ComponentScan(basePackages = {"com.lucky.seckill.shop", "com.lucky.seckill.common"})
public class ShopApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}