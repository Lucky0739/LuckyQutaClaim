package com.lucky.quota.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.notify.model.dto.NotifyTypeCountDTO;
import com.lucky.quota.notify.model.entity.Notify;
import com.lucky.quota.notify.model.vo.NotifyUnreadStatVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NotifyMapper extends BaseMapper<Notify> {

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

    /**
     * 分页查询用户消息（支持可选类型、已读状态）
     */
    List<Notify> selectUserNotifyPage(@Param("receiveUserId") Long receiveUserId,
                                      @Param("notifyType") Integer notifyType,
                                      @Param("isRead") Integer isRead,
                                      @Param("offset") Integer offset,
                                      @Param("limit") Integer limit);

    Long countUserNotify(@Param("receiveUserId") Long receiveUserId,
                         @Param("notifyType") Integer notifyType,
                         @Param("isRead") Integer isRead);

    Long countUserUnreadNotify(@Param("receiveUserId") Long receiveUserId,
                               @Param("notifyType") Integer notifyType);

    /**
     * 按用户 ID、消息类型分组统计未读数量
     */
    List<NotifyUnreadStatVO> countUnreadGroupByUserIdAndNotifyType(@Param("receiveUserId") Long receiveUserId);

    /**
     * 按消息类型分组统计用户消息总数与未读数
     */
    List<NotifyTypeCountDTO> countUserNotifySummaryGroupByType(@Param("receiveUserId") Long receiveUserId);

    Notify selectByNotifyTypeAndBusinessId(@Param("notifyType") Integer notifyType,
                                           @Param("businessId") Long businessId);

    int markAllReadByReceiveUserId(@Param("receiveUserId") Long receiveUserId,
                                   @Param("notifyType") Integer notifyType,
                                   @Param("readTime") java.time.LocalDateTime readTime,
                                   @Param("updateTime") java.time.LocalDateTime updateTime);

    /**
     * 查询用户未读消息（用于全部标记已读，仅返回 id 与类型）
     */
    List<Notify> selectUserUnreadNotifyList(@Param("receiveUserId") Long receiveUserId,
                                            @Param("notifyType") Integer notifyType);

    /**
     * 查询指定类型下全部未读消息（管理员审核类，不按 receive_user_id 过滤）
     */
    List<Notify> selectUnreadByNotifyType(@Param("notifyType") Integer notifyType);

    /**
     * 将指定类型下全部未读消息标记为已读（管理员）
     */
    int markAllReadByNotifyType(@Param("notifyType") Integer notifyType,
                                @Param("readTime") java.time.LocalDateTime readTime,
                                @Param("updateTime") java.time.LocalDateTime updateTime);
}