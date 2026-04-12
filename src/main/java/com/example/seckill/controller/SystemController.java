package com.example.seckill.controller;

import com.example.seckill.common.ApiResponse;
import com.example.seckill.service.SystemService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;

    @Value("${app.instance-name}")
    private String instanceName;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/ping")
    public ApiResponse<?> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("instance", instanceName);
        data.put("time", LocalDateTime.now());
        return ApiResponse.ok(data);
    }

    @GetMapping("/db/read")
    public ApiResponse<?> readDb() {
        return ApiResponse.ok(systemService.readDataSourceInfo());
    }

    @GetMapping("/db/write")
    public ApiResponse<?> writeDb() {
        return ApiResponse.ok(systemService.writeDataSourceInfo());
    }
}
