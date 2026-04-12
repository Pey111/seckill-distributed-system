package com.example.seckill.service;

import com.example.seckill.dto.ProductDetailResponse;
import com.example.seckill.dto.ProductSearchResponse;

import java.util.List;

public interface ProductService {

    ProductDetailResponse getProductDetail(Long productId);

    List<ProductSearchResponse> search(String keyword);

    void clearProductCache(Long productId);
}
