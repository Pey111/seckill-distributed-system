package com.example.seckill.service;

import com.example.seckill.dto.TxSeckillRequest;

import java.util.Map;

public interface DistributedTxService {

    Map<String, Object> submit(TxSeckillRequest request);

    Map<String, Object> getDetail(Long orderId);
}
