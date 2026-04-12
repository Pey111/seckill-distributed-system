package com.example.seckill.mq;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seckill.common.BusinessException;
import com.example.seckill.config.kafka.KafkaTopics;
import com.example.seckill.dto.TxOrderMessage;
import com.example.seckill.entity.OrderRecord;
import com.example.seckill.mapper.OrderRecordMapper;
import com.example.seckill.service.ProductService;
import com.example.seckill.service.TxMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Component
public class TxOrderResultConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRecordMapper orderRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ProductService productService;
    private final TxMessageService txMessageService;

    public TxOrderResultConsumer(ObjectMapper objectMapper,
                                 OrderRecordMapper orderRecordMapper,
                                 StringRedisTemplate stringRedisTemplate,
                                 ProductService productService,
                                 TxMessageService txMessageService) {
        this.objectMapper = objectMapper;
        this.orderRecordMapper = orderRecordMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.productService = productService;
        this.txMessageService = txMessageService;
    }

    @KafkaListener(topics = KafkaTopics.TX_ORDER_RESULT_TOPIC)
    @Transactional
    public void consume(String payload) {
        TxOrderMessage message = readValue(payload);
        OrderRecord order = orderRecordMapper.selectById(message.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if ("SUCCESS".equals(message.getResult())) {
            orderRecordMapper.update(null, new LambdaUpdateWrapper<OrderRecord>()
                    .eq(OrderRecord::getId, message.getOrderId())
                    .set(OrderRecord::getStatus, 1));
            saveStatus(message.getOrderId(), "SUCCESS", "下单成功，等待支付");
            productService.clearProductCache(message.getProductId());
        } else {
            orderRecordMapper.update(null, new LambdaUpdateWrapper<OrderRecord>()
                    .eq(OrderRecord::getId, message.getOrderId())
                    .set(OrderRecord::getStatus, 3));
            stringRedisTemplate.opsForValue().increment("seckill:stock:" + message.getProductId(), message.getAmount());
            stringRedisTemplate.opsForSet().remove("seckill:users:" + message.getProductId(), String.valueOf(message.getUserId()));
            saveStatus(message.getOrderId(), "FAILED", message.getMessage());
        }
        txMessageService.markDone(message.getTxNo(), KafkaTopics.TX_ORDER_RESULT_TOPIC);
    }

    private void saveStatus(Long orderId, String status, String message) {
        String key = "tx:order:status:" + orderId;
        stringRedisTemplate.opsForHash().put(key, "status", status);
        stringRedisTemplate.opsForHash().put(key, "message", message);
        stringRedisTemplate.expire(key, Duration.ofMinutes(30));
    }

    private TxOrderMessage readValue(String payload) {
        try {
            return objectMapper.readValue(payload, TxOrderMessage.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("消息解析失败");
        }
    }
}
