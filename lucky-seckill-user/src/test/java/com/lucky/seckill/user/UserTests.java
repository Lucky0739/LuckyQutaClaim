package com.lucky.seckill.user;

import com.lucky.seckill.user.domain.dataobject.UserDO;
import com.lucky.seckill.user.mapper.UserDOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class UserTests {

    @Autowired
    private UserDOMapper userDOMapper;

    @Test
    public void testInsertUser() {
        userDOMapper.insert(UserDO.builder()
                .nickname("犬小哈")
                .password("123456")
                .mobile("18019988888")
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());
    }

}