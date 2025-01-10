package com.OSS.Health;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.OSS.Health.mapper.UserMapper;
import com.OSS.Health.model.User;

@SpringBootTest
public class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testFindAllUser() {
        List<User> users = userMapper.findAllUser();
        if (users == null) {
            throw new AssertionError("User list is null");
        }
        System.out.println(users);
    }
}
