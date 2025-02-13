package com.OSS.Health.controller;

import com.OSS.Health.model.User;
import com.OSS.Health.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @RequestMapping("/getAllUser")
    public List<User> findAll(){
        return userService.findAllUser();
    }

    @RequestMapping("/getUserByUserID/{userid}")
    public List<User> findUserByUserId(@PathVariable int userid){
        return userService.findUserByUserId(userid);
    }
}

