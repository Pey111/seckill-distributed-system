package com.example.seckill.controller;

import com.example.seckill.common.ApiResponse;
import com.example.seckill.dto.CreateOrderRequest;
import com.example.seckill.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public ApiResponse<?> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok("下单成功", orderService.createOrder(request));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<?> getById(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.getById(orderId));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<?> getByUserId(@PathVariable Long userId) {
        return ApiResponse.ok(orderService.getByUserId(userId));
    }
}
