package com.example.seckill.mq;

import com.example.seckill.common.BusinessException;
import com.example.seckill.config.kafka.KafkaTopics;
import com.example.seckill.dto.SeckillMessage;
import com.example.seckill.entity.OrderRecord;
import com.example.seckill.mapper.OrderRecordMapper;
import com.example.seckill.mapper.ProductMapper;
import com.example.seckill.mapper.StockMapper;
import com.example.seckill.mapper.UserMapper;
import com.example.seckill.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class SeckillOrderConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRecordMapper orderRecordMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final StockMapper stockMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ProductService productService;

    public SeckillOrderConsumer(ObjectMapper objectMapper,
                                OrderRecordMapper orderRecordMapper,
                                UserMapper userMapper,
                                ProductMapper productMapper,
                                StockMapper stockMapper,
                                StringRedisTemplate stringRedisTemplate,
                                ProductService productService) {
        this.objectMapper = objectMapper;
        this.orderRecordMapper = orderRecordMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.stockMapper = stockMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.productService = productService;
    }

    @KafkaListener(topics = KafkaTopics.SECKILL_ORDER_TOPIC)
    @Transactional
    public void consume(String messageText) {
        SeckillMessage message = readValue(messageText);
        try {
            doConsume(message);
        } catch (Exception e) {
            rollbackRedis(message);
            updateStatus(message.getOrderId(), "FAILED", e.getMessage() == null ? "下单失败" : e.getMessage(), message);
        }
    }

    private void doConsume(SeckillMessage message) {
        if (orderRecordMapper.selectById(message.getOrderId()) != null) {
            updateStatus(message.getOrderId(), "SUCCESS", "订单已创建", message);
            return;
        }
        if (userMapper.selectById(message.getUserId()) == null) {
            throw new BusinessException("用户不存在");
        }
        if (productMapper.selectById(message.getProductId()) == null) {
            throw new BusinessException("商品不存在");
        }
        if (orderRecordMapper.countByUserIdAndProductId(message.getUserId(), message.getProductId()) > 0) {
            updateStatus(message.getOrderId(), "FAILED", "用户已下单", message);
            return;
        }
        int updated = stockMapper.reduceStock(message.getProductId(), message.getAmount());
        if (updated == 0) {
            throw new BusinessException("数据库库存不足");
        }
        OrderRecord order = new OrderRecord();
        order.setId(message.getOrderId());
        order.setUserId(message.getUserId());
        order.setProductId(message.getProductId());
        order.setAmount(message.getAmount());
        order.setStatus(1);
        order.setCreateTime(LocalDateTime.now());
        orderRecordMapper.insert(order);
        updateStatus(message.getOrderId(), "SUCCESS", "订单创建成功", message);
        productService.clearProductCache(message.getProductId());
    }

    private void rollbackRedis(SeckillMessage message) {
        stringRedisTemplate.opsForValue().increment("seckill:stock:" + message.getProductId(), message.getAmount());
        stringRedisTemplate.opsForSet().remove("seckill:users:" + message.getProductId(), String.valueOf(message.getUserId()));
    }

    private void updateStatus(Long orderId, String status, String text, SeckillMessage message) {
        String key = "seckill:order:status:" + orderId;
        stringRedisTemplate.opsForHash().put(key, "status", status);
        stringRedisTemplate.opsForHash().put(key, "message", text);
        stringRedisTemplate.opsForHash().put(key, "userId", String.valueOf(message.getUserId()));
        stringRedisTemplate.opsForHash().put(key, "productId", String.valueOf(message.getProductId()));
        stringRedisTemplate.expire(key, Duration.ofMinutes(30));
    }

    private SeckillMessage readValue(String messageText) {
        try {
            return objectMapper.readValue(messageText, SeckillMessage.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("消息解析失败");
        }
    }
}
