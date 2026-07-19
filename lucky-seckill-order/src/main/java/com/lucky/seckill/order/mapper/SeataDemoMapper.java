package com.lucky.seckill.order.mapper;

import com.lucky.seckill.order.model.entity.SeataDemoAccount;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SeataDemoMapper {

    List<SeataDemoAccount> selectAllAccounts();

    int deductBalance(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);

    int insertTxLog(@Param("id") Long id,
                    @Param("fromAccountId") Long fromAccountId,
                    @Param("toAccountId") Long toAccountId,
                    @Param("amount") BigDecimal amount,
                    @Param("remark") String remark,
                    @Param("createTime") LocalDateTime createTime);
}
