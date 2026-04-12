package com.example.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seckill.config.db.ReadOnlyDb;
import com.example.seckill.entity.Product;
import com.example.seckill.mapper.ProductMapper;
import com.example.seckill.service.ProductQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ReadOnlyDb
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductMapper productMapper;

    public ProductQueryServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public Product getById(Long productId) {
        return productMapper.selectById(productId);
    }

    @Override
    public List<Product> search(String keyword) {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .like(Product::getName, keyword)
                .or()
                .like(Product::getDescription, keyword)
                .orderByAsc(Product::getId));
    }
}
