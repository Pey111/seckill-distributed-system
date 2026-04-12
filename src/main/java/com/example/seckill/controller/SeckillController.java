package com.example.seckill.controller;

import com.example.seckill.common.ApiResponse;
import com.example.seckill.dto.SeckillRequest;
import com.example.seckill.service.SeckillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/submit")
    public ApiResponse<?> submit(@Valid @RequestBody SeckillRequest request) {
        return ApiResponse.ok("请求已提交", seckillService.submit(request));
    }

    @GetMapping("/status/{orderId}")
    public ApiResponse<?> status(@PathVariable Long orderId) {
        return ApiResponse.ok(seckillService.getStatus(orderId));
    }
}
