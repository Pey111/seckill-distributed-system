package com.example.seckill.service;

import com.example.seckill.entity.Product;

import java.util.List;

public interface ProductQueryService {

    Product getById(Long productId);

    List<Product> search(String keyword);
}
