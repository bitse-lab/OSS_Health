package com.OSS.Health.mapper;

import com.OSS.Health.model.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    public List<User> findAllUser();
    public List<User> findUserByUserId(int userid);
}

