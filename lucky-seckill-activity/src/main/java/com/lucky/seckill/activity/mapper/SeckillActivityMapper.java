package com.lucky.seckill.activity.mapper;

import com.lucky.seckill.activity.model.entity.SeckillActivity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeckillActivityMapper {

    SeckillActivity selectByPrimaryKey(Long id);

    List<SeckillActivity> selectAll();

    List<SeckillActivity> selectNeedWarmUp(@Param("now") LocalDateTime now);

    List<SeckillActivity> selectNeedEnd(@Param("now") LocalDateTime now);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("updateTime") LocalDateTime updateTime);
}
