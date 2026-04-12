package com.example.seckill.controller;

import com.example.seckill.common.ApiResponse;
import com.example.seckill.dto.TxPayRequest;
import com.example.seckill.dto.TxSeckillRequest;
import com.example.seckill.service.DistributedTxService;
import com.example.seckill.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tx")
public class DistributedTxController {

    private final DistributedTxService distributedTxService;
    private final PaymentService paymentService;

    public DistributedTxController(DistributedTxService distributedTxService, PaymentService paymentService) {
        this.distributedTxService = distributedTxService;
        this.paymentService = paymentService;
    }

    @PostMapping("/seckill/submit")
    public ApiResponse<?> submit(@Valid @RequestBody TxSeckillRequest request) {
        return ApiResponse.ok("事务下单请求已提交", distributedTxService.submit(request));
    }

    @PostMapping("/pay")
    public ApiResponse<?> pay(@Valid @RequestBody TxPayRequest request) {
        return ApiResponse.ok("支付请求已提交", paymentService.submit(request));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<?> detail(@PathVariable Long orderId) {
        return ApiResponse.ok(distributedTxService.getDetail(orderId));
    }

    @GetMapping("/payment/{orderId}")
    public ApiResponse<?> payment(@PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.getByOrderId(orderId));
    }
}
