package com.example.seckill.controller;

import com.example.seckill.common.ApiResponse;
import com.example.seckill.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.getProductDetail(id));
    }

    @GetMapping("/search")
    public ApiResponse<?> search(@RequestParam String keyword) {
        return ApiResponse.ok(productService.search(keyword));
    }
}
