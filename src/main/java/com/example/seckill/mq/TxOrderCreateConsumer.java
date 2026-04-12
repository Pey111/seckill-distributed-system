package com.example.seckill.mq;

import com.example.seckill.common.BusinessException;
import com.example.seckill.config.kafka.KafkaTopics;
import com.example.seckill.dto.TxOrderMessage;
import com.example.seckill.entity.OrderRecord;
import com.example.seckill.mapper.OrderRecordMapper;
import com.example.seckill.service.TxMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class TxOrderCreateConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRecordMapper orderRecordMapper;
    private final TxMessageService txMessageService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public TxOrderCreateConsumer(ObjectMapper objectMapper,
                                 OrderRecordMapper orderRecordMapper,
                                 TxMessageService txMessageService,
                                 KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.orderRecordMapper = orderRecordMapper;
        this.txMessageService = txMessageService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.TX_ORDER_CREATE_TOPIC)
    @Transactional
    public void consume(String payload) {
        TxOrderMessage message = readValue(payload);
        if (orderRecordMapper.selectById(message.getOrderId()) == null) {
            OrderRecord order = new OrderRecord();
            order.setId(message.getOrderId());
            order.setUserId(message.getUserId());
            order.setProductId(message.getProductId());
            order.setAmount(message.getAmount());
            order.setStatus(0);
            order.setCreateTime(LocalDateTime.now());
            orderRecordMapper.insert(order);
        }
        txMessageService.markDone(message.getTxNo(), KafkaTopics.TX_ORDER_CREATE_TOPIC);
        txMessageService.save(message.getTxNo(), String.valueOf(message.getOrderId()), KafkaTopics.TX_STOCK_DEDUCT_TOPIC, payload);
        send(message.getTxNo(), KafkaTopics.TX_STOCK_DEDUCT_TOPIC, payload);
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

    private TxOrderMessage readValue(String payload) {
        try {
            return objectMapper.readValue(payload, TxOrderMessage.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("消息解析失败");
        }
    }
}
