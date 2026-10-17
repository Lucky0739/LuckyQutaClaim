package com.lucky.quota.user;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan({
        "com.lucky.quota.user.mapper",
        "com.lucky.quota.user.kb.mapper",
        "com.lucky.quota.common.mapper",
        "com.lucky.quota.notify.mapper"
})
@ComponentScan(basePackages = {
        "com.lucky.quota.user",
        "com.lucky.quota.notify",
        "com.lucky.quota.common"
})
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}