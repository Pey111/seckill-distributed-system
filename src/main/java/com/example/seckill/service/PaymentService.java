package com.example.seckill.service;

import com.example.seckill.dto.TxPayRequest;

import java.util.Map;

public interface PaymentService {

    Map<String, Object> submit(TxPayRequest request);

    Map<String, Object> getByOrderId(Long orderId);
}
