package com.lucky.quota.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.activity.model.entity.QuotaActivity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuotaActivityMapper extends BaseMapper<QuotaActivity> {

    QuotaActivity selectByPrimaryKey(Long id);

    List<QuotaActivity> selectAll();

    List<QuotaActivity> selectNeedWarmUp(@Param("now") LocalDateTime now);

    /** 进行中且仍在时间窗内的场次，用于 Redis 预热丢失后的补预热 */
    List<QuotaActivity> selectInProgress(@Param("now") LocalDateTime now);

    List<QuotaActivity> selectNeedEnd(@Param("now") LocalDateTime now);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("updateTime") LocalDateTime updateTime);
}
