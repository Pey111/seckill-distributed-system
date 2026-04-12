package com.example.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.common.BusinessException;
import com.example.seckill.dto.LoginRequest;
import com.example.seckill.dto.RegisterRequest;
import com.example.seckill.entity.User;
import com.example.seckill.mapper.UserMapper;
import com.example.seckill.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Map<String, Object> register(RegisterRequest request) {
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()));
        if (exists != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(md5(request.password()));
        user.setPhone(request.phone());
        userMapper.insert(user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("phone", user.getPhone());
        return data;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()));
        if (user == null || !user.getPassword().equals(md5(request.password()))) {
            throw new BusinessException("用户名或密码错误");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("phone", user.getPhone());
        return data;
    }

    private String md5(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}
