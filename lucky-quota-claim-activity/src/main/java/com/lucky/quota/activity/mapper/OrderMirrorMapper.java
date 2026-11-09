package com.lucky.quota.activity.mapper;

import com.lucky.quota.activity.model.entity.MirroredOrderRow;
import org.apache.ibatis.annotations.Param;

public interface OrderMirrorMapper {

    int countByOrderNo(@Param("orderNo") String orderNo);

    int insertMirroredOrder(MirroredOrderRow row);

    int updateMirroredOrder(MirroredOrderRow row);
}
