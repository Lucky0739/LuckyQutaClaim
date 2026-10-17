package com.lucky.quota.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.common.model.entity.ImageCleanTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ImageCleanTaskMapper extends BaseMapper<ImageCleanTask> {

    int insertSelective(ImageCleanTask record);

    int countPendingByImageUrl(@Param("imageUrl") String imageUrl);

    List<ImageCleanTask> selectExpiredPendingTasks(@Param("limit") int limit);

    int updateStatus(ImageCleanTask record);
}
