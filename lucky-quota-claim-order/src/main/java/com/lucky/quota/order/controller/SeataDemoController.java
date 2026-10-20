package com.lucky.quota.order.controller;

import com.lucky.quota.common.utils.Response;
import com.lucky.quota.order.model.entity.SeataDemoAccount;
import com.lucky.quota.order.service.SeataDemoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Seata 分布式事务演示
 */
@RestController
@RequestMapping("/order/seata/demo")
@Slf4j
public class SeataDemoController {

    @Resource
    private SeataDemoService seataDemoService;

    @GetMapping("/accounts")
    public Response<List<SeataDemoAccount>> listAccounts() {
        return seataDemoService.listAccounts();
    }

    @PostMapping("/transfer")
    public Response<Map<String, Object>> transfer(
            @RequestParam(value = "amount", defaultValue = "100") BigDecimal amount,
            @RequestParam(value = "fail", defaultValue = "false") boolean fail) {
        log.info("==> Seata 演示转账, amount={}, fail={}", amount, fail);
        return seataDemoService.transfer(amount, fail);
    }
}
