package com.example.seckill.dto;

import jakarta.validation.constraints.NotNull;

public record TxPayRequest(
        @NotNull(message = "订单不能为空") Long orderId,
        @NotNull(message = "用户不能为空") Long userId
) {
}
