package com.example.seckill.service;

public interface StockService {

    void reduceStock(Long productId, Integer amount);
}
