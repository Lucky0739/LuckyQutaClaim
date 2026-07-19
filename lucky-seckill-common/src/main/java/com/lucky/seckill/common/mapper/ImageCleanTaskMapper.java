package com.lucky.seckill.common.mapper;

import com.lucky.seckill.common.model.entity.ImageCleanTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ImageCleanTaskMapper {

    int insertSelective(ImageCleanTask record);

    int countPendingByImageUrl(@Param("imageUrl") String imageUrl);

    List<ImageCleanTask> selectExpiredPendingTasks(@Param("limit") int limit);

    int updateStatus(ImageCleanTask record);
}
