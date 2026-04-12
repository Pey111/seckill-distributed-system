package com.example.seckill.service;

import com.example.seckill.dto.LoginRequest;
import com.example.seckill.dto.RegisterRequest;

import java.util.Map;

public interface UserService {

    Map<String, Object> register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);
}
