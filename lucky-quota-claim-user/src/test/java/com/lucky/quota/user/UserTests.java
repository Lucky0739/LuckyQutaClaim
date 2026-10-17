package com.lucky.quota.user;

import com.lucky.quota.user.mapper.UserMapper;
import com.lucky.quota.user.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class UserTests {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testInsertUser() {
        userMapper.insert(User.builder()
                .nickname("测试用户")
                .password("123456")
                .mobile("18019988888")
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());
    }

}