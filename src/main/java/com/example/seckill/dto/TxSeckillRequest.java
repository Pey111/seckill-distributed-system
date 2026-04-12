package com.example.seckill.dto;

import jakarta.validation.constraints.NotNull;

public record TxSeckillRequest(
        @NotNull(message = "用户不能为空") Long userId,
        @NotNull(message = "商品不能为空") Long productId
) {
}
