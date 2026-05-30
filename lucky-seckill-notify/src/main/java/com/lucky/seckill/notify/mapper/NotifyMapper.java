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
     * 分页查询指定类型的通知列表
     */
    List<Notify> selectByNotifyTypePage(@Param("notifyType") Integer notifyType,
                                        @Param("offset") Integer offset, @Param("limit") Integer limit);
    /**
     * 查询指定类型通知的总记录数
     */
    Long countByNotifyType(@Param("notifyType") Integer notifyType);

    /**
     * 分页查询指定用户、指定类型的通知列表
     */
    List<Notify> selectByReceiveUserIdAndNotifyTypePage(@Param("receiveUserId") Long receiveUserId,
                                                        @Param("notifyType") Integer notifyType,
                                                        @Param("offset") Integer offset,
                                                        @Param("limit") Integer limit);

    Long countByReceiveUserIdAndNotifyType(@Param("receiveUserId") Long receiveUserId,
                                           @Param("notifyType") Integer notifyType);
}