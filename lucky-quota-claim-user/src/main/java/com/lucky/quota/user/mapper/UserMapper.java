package com.lucky.quota.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.user.model.entity.User;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.Date;
import java.util.List;

public interface UserMapper extends BaseMapper<User> {
    int deleteByPrimaryKey(Long id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

    /**
     * 根据手机号查询用户 ID（仅判断手机号是否已注册，不返回完整用户信息）
     *
     * @param mobile
     * @return
     */
    Long selectIdByMobile(String mobile);

    /**
     * 根据手机号查询用户信息
     *
     * @param mobile
     * @return
     */
    User selectByMobile(String mobile);

    /**
     * 根据手机号查询已逻辑删除的用户 ID
     *
     * @param mobile
     * @return 已删除用户的 ID，如果不存在则返回 null
     */
    Long selectDeletedIdByMobile(String mobile);

    /**
     * 管理员分页查询用户列表（支持多条件筛选）
     */
    List<User> selectUserListByAdmin(
            @Param("nickname") String nickname,
            @Param("mobile") String mobile,
            @Param("status") Integer status,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime
    );

    /**
     * 按昵称前缀查询用户（压测批量登录使用）
     */
    List<User> selectByNicknamePrefix(@Param("nicknamePrefix") String nicknamePrefix);
}
