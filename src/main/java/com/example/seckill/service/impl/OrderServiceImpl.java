package com.example.seckill.service.impl;

import com.example.seckill.common.BusinessException;
import com.example.seckill.dto.OrderResponse;
import com.example.seckill.dto.CreateOrderRequest;
import com.example.seckill.entity.OrderRecord;
import com.example.seckill.entity.Product;
import com.example.seckill.entity.User;
import com.example.seckill.mapper.OrderRecordMapper;
import com.example.seckill.mapper.ProductMapper;
import com.example.seckill.mapper.UserMapper;
import com.example.seckill.service.OrderService;
import com.example.seckill.service.ProductService;
import com.example.seckill.service.StockService;
import com.example.seckill.util.SnowflakeIdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRecordMapper orderRecordMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final StockService stockService;
    private final ProductService productService;
    private final SnowflakeIdWorker snowflakeIdWorker;

    public OrderServiceImpl(OrderRecordMapper orderRecordMapper,
                            UserMapper userMapper,
                            ProductMapper productMapper,
                            StockService stockService,
                            ProductService productService,
                            SnowflakeIdWorker snowflakeIdWorker) {
        this.orderRecordMapper = orderRecordMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.stockService = stockService;
        this.productService = productService;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    @Override
    @Transactional
    public Map<String, Object> createOrder(CreateOrderRequest request) {
        User user = userMapper.selectById(request.userId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        Product product = productMapper.selectById(request.productId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        stockService.reduceStock(request.productId(), request.amount());
        OrderRecord order = new OrderRecord();
        order.setId(snowflakeIdWorker.nextId());
        order.setUserId(request.userId());
        order.setProductId(request.productId());
        order.setAmount(request.amount());
        order.setStatus(1);
        order.setCreateTime(LocalDateTime.now());
        orderRecordMapper.insert(order);
        productService.clearProductCache(request.productId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", order.getId());
        data.put("userId", order.getUserId());
        data.put("productId", order.getProductId());
        data.put("amount", order.getAmount());
        data.put("status", order.getStatus());
        data.put("createTime", order.getCreateTime());
        return data;
    }

    @Override
    public OrderResponse getById(Long orderId) {
        OrderRecord order = orderRecordMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return convert(order);
    }

    @Override
    public List<OrderResponse> getByUserId(Long userId) {
        List<OrderRecord> orders = orderRecordMapper.selectByUserId(userId);
        List<OrderResponse> result = new ArrayList<>();
        for (OrderRecord order : orders) {
            result.add(convert(order));
        }
        return result;
    }

    private OrderResponse convert(OrderRecord order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setProductId(order.getProductId());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatus());
        response.setStatusText(statusText(order.getStatus()));
        response.setCreateTime(order.getCreateTime());
        return response;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "PENDING_STOCK";
            case 1 -> "UNPAID";
            case 2 -> "PAID";
            case 3 -> "CANCELLED";
            default -> "UNKNOWN";
        };
    }
}
