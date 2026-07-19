package com.lucky.seckill.user.controller;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.service.SeataDemoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/user/seata/demo")
@Slf4j
public class SeataDemoController {

    @Resource
    private SeataDemoService seataDemoService;

    @PostMapping("/credit")
    public Response<Boolean> credit(@RequestParam("accountId") Long accountId,
                                    @RequestParam("amount") BigDecimal amount) {
        return seataDemoService.credit(accountId, amount);
    }
}
