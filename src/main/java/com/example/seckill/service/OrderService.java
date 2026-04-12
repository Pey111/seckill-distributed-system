package com.example.seckill.service;

import com.example.seckill.dto.CreateOrderRequest;
import com.example.seckill.dto.OrderResponse;

import java.util.List;
import java.util.Map;

public interface OrderService {

    Map<String, Object> createOrder(CreateOrderRequest request);

    OrderResponse getById(Long orderId);

    List<OrderResponse> getByUserId(Long userId);
}
