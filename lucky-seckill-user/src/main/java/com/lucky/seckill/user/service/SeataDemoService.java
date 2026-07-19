package com.lucky.seckill.user.service;

import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.user.mapper.SeataDemoMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
public class SeataDemoService {

    @Resource
    private SeataDemoMapper seataDemoMapper;

    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> credit(Long accountId, BigDecimal amount) {
        log.info("[Seata] 用户服务分支事务入账, accountId={}, amount={}", accountId, amount);
        int rows = seataDemoMapper.addBalance(accountId, amount);
        return rows == 1 ? Response.success(true) : Response.fail("账户不存在");
    }
}
