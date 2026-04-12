package com.example.seckill.service;

import java.util.Map;

public interface SystemService {

    Map<String, Object> readDataSourceInfo();

    Map<String, Object> writeDataSourceInfo();
}
