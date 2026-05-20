package com.lucky.seckill.notify.mapper;

import com.lucky.seckill.notify.model.entity.Notify;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NotifyMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Notify record);

    int insertSelective(Notify record);

    Notify selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Notify record);

    int updateByPrimaryKey(Notify record);

    List<Notify> selectByNotifyTypeAndIsRead(@Param("notifyType") Integer notifyType, @Param("isRead") Integer isRead);

    List<Notify> selectByReceiveUserId(@Param("receiveUserId") Long receiveUserId);

    /**
     * 查询未读的指定类型、业务 ID 通知
     */
    Notify selectUnreadByNotifyTypeAndBusinessId(@Param("notifyType") Integer notifyType,
                                                 @Param("businessId") Long businessId);
}