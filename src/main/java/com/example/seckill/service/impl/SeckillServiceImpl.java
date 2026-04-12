package com.example.seckill.service.impl;

import com.example.seckill.common.BusinessException;
import com.example.seckill.config.kafka.KafkaTopics;
import com.example.seckill.dto.SeckillMessage;
import com.example.seckill.dto.SeckillRequest;
import com.example.seckill.dto.SeckillSubmitResponse;
import com.example.seckill.entity.Product;
import com.example.seckill.entity.Stock;
import com.example.seckill.entity.User;
import com.example.seckill.mapper.ProductMapper;
import com.example.seckill.mapper.StockMapper;
import com.example.seckill.mapper.UserMapper;
import com.example.seckill.service.SeckillService;
import com.example.seckill.util.SnowflakeIdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SeckillServiceImpl implements SeckillService {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final StockMapper stockMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<Long> seckillScript;

    public SeckillServiceImpl(UserMapper userMapper,
                              ProductMapper productMapper,
                              StockMapper stockMapper,
                              StringRedisTemplate stringRedisTemplate,
                              SnowflakeIdWorker snowflakeIdWorker,
                              KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.stockMapper = stockMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.seckillScript = new DefaultRedisScript<>();
        this.seckillScript.setScriptText("""
                if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
                    return 2
                end
                local stock = redis.call('GET', KEYS[1])
                if not stock then
                    return 3
                end
                if tonumber(stock) < tonumber(ARGV[2]) then
                    return 0
                end
                redis.call('DECRBY', KEYS[1], ARGV[2])
                redis.call('SADD', KEYS[2], ARGV[1])
                return 1
                """);
        this.seckillScript.setResultType(Long.class);
    }

    @Override
    public SeckillSubmitResponse submit(SeckillRequest request) {
        User user = userMapper.selectById(request.userId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        Product product = productMapper.selectById(request.productId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        initStockIfAbsent(request.productId());
        String stockKey = stockKey(request.productId());
        String usersKey = usersKey(request.productId());
        Long result = stringRedisTemplate.execute(seckillScript,
                Arrays.asList(stockKey, usersKey),
                String.valueOf(request.userId()),
                "1");
        if (Long.valueOf(2L).equals(result)) {
            throw new BusinessException("同一用户只能秒杀一次");
        }
        if (Long.valueOf(0L).equals(result)) {
            throw new BusinessException("商品已售罄");
        }
        if (Long.valueOf(3L).equals(result)) {
            throw new BusinessException("秒杀库存未初始化");
        }
        Long orderId = snowflakeIdWorker.nextId();
        String statusKey = statusKey(orderId);
        stringRedisTemplate.opsForHash().put(statusKey, "status", "PENDING");
        stringRedisTemplate.opsForHash().put(statusKey, "message", "排队中");
        stringRedisTemplate.opsForHash().put(statusKey, "userId", String.valueOf(request.userId()));
        stringRedisTemplate.opsForHash().put(statusKey, "productId", String.valueOf(request.productId()));
        stringRedisTemplate.expire(statusKey, Duration.ofMinutes(30));
        SeckillMessage message = new SeckillMessage();
        message.setOrderId(orderId);
        message.setUserId(request.userId());
        message.setProductId(request.productId());
        message.setAmount(1);
        kafkaTemplate.send(KafkaTopics.SECKILL_ORDER_TOPIC, String.valueOf(orderId), writeValue(message));
        SeckillSubmitResponse response = new SeckillSubmitResponse();
        response.setOrderId(orderId);
        response.setStatus("PENDING");
        response.setMessage("请求已进入消息队列");
        return response;
    }

    @Override
    public Map<String, Object> getStatus(Long orderId) {
        Map<Object, Object> values = stringRedisTemplate.opsForHash().entries(statusKey(orderId));
        if (values == null || values.isEmpty()) {
            throw new BusinessException("订单状态不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId);
        data.put("status", values.get("status"));
        data.put("message", values.get("message"));
        data.put("userId", values.get("userId"));
        data.put("productId", values.get("productId"));
        return data;
    }

    private void initStockIfAbsent(Long productId) {
        String stockKey = stockKey(productId);
        Boolean exists = stringRedisTemplate.hasKey(stockKey);
        if (Boolean.TRUE.equals(exists)) {
            return;
        }
        Stock stockRecord = stockMapper.selectById(productId);
        Integer stock = stockRecord == null ? 0 : stockRecord.getStockCount();
        stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(stock));
    }

    private String writeValue(SeckillMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new BusinessException("消息发送失败");
        }
    }

    private String stockKey(Long productId) {
        return "seckill:stock:" + productId;
    }

    private String usersKey(Long productId) {
        return "seckill:users:" + productId;
    }

    private String statusKey(Long orderId) {
        return "seckill:order:status:" + orderId;
    }
}
