package com.lucky.quota.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.order.model.entity.SeataDemoAccount;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SeataDemoMapper extends BaseMapper<SeataDemoAccount> {

    List<SeataDemoAccount> selectAllAccounts();

    int deductBalance(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);

    int insertTxLog(@Param("id") Long id,
                    @Param("fromAccountId") Long fromAccountId,
                    @Param("toAccountId") Long toAccountId,
                    @Param("amount") BigDecimal amount,
                    @Param("remark") String remark,
                    @Param("createTime") LocalDateTime createTime);
}
