package com.example.seckill.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "用户不能为空") Long userId,
        @NotNull(message = "商品不能为空") Long productId,
        @NotNull(message = "购买数量不能为空") @Min(value = 1, message = "购买数量必须大于0") Integer amount
) {
}
