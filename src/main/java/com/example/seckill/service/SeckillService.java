package com.example.seckill.service;

import com.example.seckill.dto.SeckillRequest;
import com.example.seckill.dto.SeckillSubmitResponse;

import java.util.Map;

public interface SeckillService {

    SeckillSubmitResponse submit(SeckillRequest request);

    Map<String, Object> getStatus(Long orderId);
}
