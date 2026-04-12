package com.example.seckill.service.impl;

import com.example.seckill.config.db.ReadOnlyDb;
import com.example.seckill.service.SystemService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemServiceImpl implements SystemService {

    private final JdbcTemplate jdbcTemplate;

    public SystemServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @ReadOnlyDb
    public Map<String, Object> readDataSourceInfo() {
        return load("read");
    }

    @Override
    public Map<String, Object> writeDataSourceInfo() {
        return load("write");
    }

    private Map<String, Object> load(String mode) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", mode);
        data.put("hostname", jdbcTemplate.queryForObject("select @@hostname", String.class));
        data.put("database", jdbcTemplate.queryForObject("select database()", String.class));
        return data;
    }
}
