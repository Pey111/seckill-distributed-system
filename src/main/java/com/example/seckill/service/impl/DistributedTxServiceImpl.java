package com.example.seckill.service.impl;

import com.example.seckill.common.BusinessException;
import com.example.seckill.config.kafka.KafkaTopics;
import com.example.seckill.dto.TxOrderMessage;
import com.example.seckill.dto.TxSeckillRequest;
import com.example.seckill.entity.OrderRecord;
import com.example.seckill.entity.Product;
import com.example.seckill.entity.Stock;
import com.example.seckill.entity.TxMessageRecord;
import com.example.seckill.entity.User;
import com.example.seckill.mapper.OrderRecordMapper;
import com.example.seckill.mapper.ProductMapper;
import com.example.seckill.mapper.StockMapper;
import com.example.seckill.mapper.UserMapper;
import com.example.seckill.service.DistributedTxService;
import com.example.seckill.service.PaymentService;
import com.example.seckill.service.TxMessageService;
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
import java.util.List;
import java.util.Map;

@Service
public class DistributedTxServiceImpl implements DistributedTxService {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final StockMapper stockMapper;
    private final OrderRecordMapper orderRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final TxMessageService txMessageService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final DefaultRedisScript<Long> seckillScript;

    public DistributedTxServiceImpl(UserMapper userMapper,
                                    ProductMapper productMapper,
                                    StockMapper stockMapper,
                                    OrderRecordMapper orderRecordMapper,
                                    StringRedisTemplate stringRedisTemplate,
                                    SnowflakeIdWorker snowflakeIdWorker,
                                    TxMessageService txMessageService,
                                    KafkaTemplate<String, String> kafkaTemplate,
                                    ObjectMapper objectMapper,
                                    PaymentService paymentService) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.stockMapper = stockMapper;
        this.orderRecordMapper = orderRecordMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.txMessageService = txMessageService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
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
    public Map<String, Object> submit(TxSeckillRequest request) {
        User user = userMapper.selectById(request.userId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        Product product = productMapper.selectById(request.productId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        initStockIfAbsent(request.productId());
        Long result = stringRedisTemplate.execute(seckillScript,
                Arrays.asList(stockKey(request.productId()), usersKey(request.productId())),
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
        String txNo = "ORDER_" + orderId;
        TxOrderMessage message = new TxOrderMessage();
        message.setTxNo(txNo);
        message.setOrderId(orderId);
        message.setUserId(request.userId());
        message.setProductId(request.productId());
        message.setAmount(1);
        saveStatus(orderId, "PENDING", "事务下单已提交");
        String payload = writeValue(message);
        txMessageService.save(txNo, String.valueOf(orderId), KafkaTopics.TX_ORDER_CREATE_TOPIC, payload);
        send(txNo, KafkaTopics.TX_ORDER_CREATE_TOPIC, payload);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId);
        data.put("txNo", txNo);
        data.put("status", "PENDING");
        data.put("message", "事务下单消息已发送");
        return data;
    }

    @Override
    public Map<String, Object> getDetail(Long orderId) {
        OrderRecord order = orderRecordMapper.selectById(orderId);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("order", order);
        status.put("txMessages", simplify(txMessageService.listByBusinessKey(String.valueOf(orderId))));
        try {
            status.put("payment", paymentService.getByOrderId(orderId));
        } catch (Exception e) {
            status.put("payment", null);
        }
        status.put("redisStatus", stringRedisTemplate.opsForHash().entries(statusKey(orderId)));
        return status;
    }

    private List<Map<String, Object>> simplify(List<TxMessageRecord> records) {
        return records.stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("txNo", item.getTxNo());
            map.put("topic", item.getTopic());
            map.put("status", item.getStatus());
            map.put("retryCount", item.getRetryCount());
            map.put("lastError", item.getLastError());
            return map;
        }).toList();
    }

    private void send(String txNo, String topic, String payload) {
        try {
            kafkaTemplate.send(topic, txNo, payload);
            txMessageService.markSent(txNo, topic);
        } catch (Exception e) {
            txMessageService.markFailed(txNo, topic, e.getMessage());
            throw new BusinessException("消息发送失败");
        }
    }

    private void initStockIfAbsent(Long productId) {
        String key = stockKey(productId);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            return;
        }
        Stock stockRecord = stockMapper.selectById(productId);
        int stock = stockRecord == null ? 0 : stockRecord.getStockCount();
        stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(stock));
    }

    private String writeValue(TxOrderMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new BusinessException("事务消息序列化失败");
        }
    }

    private void saveStatus(Long orderId, String status, String message) {
        String key = statusKey(orderId);
        stringRedisTemplate.opsForHash().put(key, "status", status);
        stringRedisTemplate.opsForHash().put(key, "message", message);
        stringRedisTemplate.expire(key, Duration.ofMinutes(30));
    }

    private String stockKey(Long productId) {
        return "seckill:stock:" + productId;
    }

    private String usersKey(Long productId) {
        return "seckill:users:" + productId;
    }

    private String statusKey(Long orderId) {
        return "tx:order:status:" + orderId;
    }
}
