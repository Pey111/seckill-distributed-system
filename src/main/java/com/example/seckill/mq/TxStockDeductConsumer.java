package com.example.seckill.mq;

import com.example.seckill.common.BusinessException;
import com.example.seckill.config.kafka.KafkaTopics;
import com.example.seckill.dto.TxOrderMessage;
import com.example.seckill.mapper.StockMapper;
import com.example.seckill.service.TxMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TxStockDeductConsumer {

    private final ObjectMapper objectMapper;
    private final StockMapper stockMapper;
    private final TxMessageService txMessageService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public TxStockDeductConsumer(ObjectMapper objectMapper,
                                 StockMapper stockMapper,
                                 TxMessageService txMessageService,
                                 KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.stockMapper = stockMapper;
        this.txMessageService = txMessageService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.TX_STOCK_DEDUCT_TOPIC)
    public void consume(String payload) {
        TxOrderMessage message = readValue(payload);
        try {
            int updated = stockMapper.reduceStock(message.getProductId(), message.getAmount());
            message.setResult(updated > 0 ? "SUCCESS" : "FAILED");
            message.setMessage(updated > 0 ? "库存扣减成功" : "数据库库存不足");
            txMessageService.markDone(message.getTxNo(), KafkaTopics.TX_STOCK_DEDUCT_TOPIC);
            publishResult(message);
        } catch (Exception e) {
            txMessageService.markFailed(message.getTxNo(), KafkaTopics.TX_STOCK_DEDUCT_TOPIC, e.getMessage());
            throw e;
        }
    }

    private void publishResult(TxOrderMessage message) {
        String payload = writeValue(message);
        txMessageService.save(message.getTxNo(), String.valueOf(message.getOrderId()), KafkaTopics.TX_ORDER_RESULT_TOPIC, payload);
        try {
            kafkaTemplate.send(KafkaTopics.TX_ORDER_RESULT_TOPIC, message.getTxNo(), payload);
            txMessageService.markSent(message.getTxNo(), KafkaTopics.TX_ORDER_RESULT_TOPIC);
        } catch (Exception e) {
            txMessageService.markFailed(message.getTxNo(), KafkaTopics.TX_ORDER_RESULT_TOPIC, e.getMessage());
            throw new BusinessException("结果消息发送失败");
        }
    }

    private TxOrderMessage readValue(String payload) {
        try {
            return objectMapper.readValue(payload, TxOrderMessage.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("消息解析失败");
        }
    }

    private String writeValue(TxOrderMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new BusinessException("消息序列化失败");
        }
    }
}
