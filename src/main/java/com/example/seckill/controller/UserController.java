package com.example.seckill.controller;

import com.example.seckill.common.ApiResponse;
import com.example.seckill.dto.LoginRequest;
import com.example.seckill.dto.RegisterRequest;
import com.example.seckill.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("注册成功", userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("登录成功", userService.login(request));
    }
}
