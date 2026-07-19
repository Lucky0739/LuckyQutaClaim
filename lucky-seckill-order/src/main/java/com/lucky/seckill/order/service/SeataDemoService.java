package com.lucky.seckill.order.service;

import cn.hutool.core.util.IdUtil;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.utils.Response;
import com.lucky.seckill.order.feign.SeataDemoFeignClient;
import com.lucky.seckill.order.mapper.SeataDemoMapper;
import com.lucky.seckill.order.model.entity.SeataDemoAccount;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SeataDemoService {

    private static final Long ACCOUNT_A = 1L;
    private static final Long ACCOUNT_B = 2L;

    @Resource
    private SeataDemoMapper seataDemoMapper;
    @Resource
    private SeataDemoFeignClient seataDemoFeignClient;

    public Response<List<SeataDemoAccount>> listAccounts() {
        return Response.success(seataDemoMapper.selectAllAccounts());
    }

    @GlobalTransactional(name = "seata-demo-transfer", rollbackFor = Exception.class)
    public Response<Map<String, Object>> transfer(BigDecimal amount, boolean fail) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "转账金额必须大于 0");
        }

        log.info("[Seata] 开始全局事务, amount={}, fail={}", amount, fail);

        int deductRows = seataDemoMapper.deductBalance(ACCOUNT_A, amount);
        if (deductRows != 1) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID, "账户 A 余额不足");
        }

        Response<Boolean> creditResult = seataDemoFeignClient.credit(ACCOUNT_B, amount);
        if (creditResult == null || !creditResult.isSuccess()
                || creditResult.getData() == null || !creditResult.getData()) {
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "用户服务入账失败");
        }

        seataDemoMapper.insertTxLog(
                IdUtil.getSnowflakeNextId(),
                ACCOUNT_A,
                ACCOUNT_B,
                amount,
                fail ? "演示异常回滚" : "Seata 演示转账成功",
                LocalDateTime.now()
        );

        if (fail) {
            throw new BizException(ResponseCodeEnum.SEATA_DEMO_ROLLBACK,
                    "已回滚，Seata 全局事务已撤销");
        }
        Map<String, Object> result = new HashMap<>(4);
        result.put("amount", amount);
        result.put("message", "全局事务提交成功");
        result.put("accounts", seataDemoMapper.selectAllAccounts());
        log.info("[Seata] 全局事务提交成功, amount={}", amount);
        return Response.success(result);
    }
}
