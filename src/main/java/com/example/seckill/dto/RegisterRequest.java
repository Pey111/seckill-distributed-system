package com.example.seckill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空") @Size(max = 20, message = "用户名长度不能超过20") String username,
        @NotBlank(message = "密码不能为空") @Size(min = 6, max = 20, message = "密码长度应为6到20位") String password,
        @NotBlank(message = "手机号不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone
) {
}
