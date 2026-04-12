package com.example.seckill.service.impl;

import com.example.seckill.common.BusinessException;
import com.example.seckill.mapper.StockMapper;
import com.example.seckill.service.ProductService;
import com.example.seckill.service.StockService;
import org.springframework.stereotype.Service;

@Service
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;
    private final ProductService productService;

    public StockServiceImpl(StockMapper stockMapper, ProductService productService) {
        this.stockMapper = stockMapper;
        this.productService = productService;
    }

    @Override
    public void reduceStock(Long productId, Integer amount) {
        int updated = stockMapper.reduceStock(productId, amount);
        if (updated == 0) {
            throw new BusinessException("库存不足");
        }
        productService.clearProductCache(productId);
    }
}
