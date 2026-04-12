package com.example.seckill.controller;

import com.example.seckill.common.ApiResponse;
import com.example.seckill.dto.ReduceStockRequest;
import com.example.seckill.service.StockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/reduce")
    public ApiResponse<?> reduce(@Valid @RequestBody ReduceStockRequest request) {
        stockService.reduceStock(request.productId(), request.amount());
        return ApiResponse.ok("扣减成功", null);
    }
}
