package com.example.seckill.mq;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seckill.common.BusinessException;
import com.example.seckill.config.kafka.KafkaTopics;
import com.example.seckill.dto.TxPaymentMessage;
import com.example.seckill.entity.PaymentRecord;
import com.example.seckill.mapper.PaymentRecordMapper;
import com.example.seckill.service.TxMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class TxPaymentConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final TxMessageService txMessageService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public TxPaymentConsumer(ObjectMapper objectMapper,
                             PaymentRecordMapper paymentRecordMapper,
                             TxMessageService txMessageService,
                             KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.txMessageService = txMessageService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.TX_PAYMENT_TOPIC)
    @Transactional
    public void consume(String payload) {
        TxPaymentMessage message = readValue(payload);
        PaymentRecord payment = paymentRecordMapper.selectByOrderId(message.getOrderId());
        if (payment == null) {
            throw new BusinessException("支付记录不存在");
        }
        paymentRecordMapper.update(null, new LambdaUpdateWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, message.getOrderId())
                .set(PaymentRecord::getStatus, 0)
                .set(PaymentRecord::getUpdateTime, LocalDateTime.now()));
        txMessageService.markDone(message.getTxNo(), KafkaTopics.TX_PAYMENT_TOPIC);
        message.setResult("SUCCESS");
        message.setMessage("支付服务处理成功");
        publish(message);
    }

    private void publish(TxPaymentMessage message) {
        String payload = writeValue(message);
        txMessageService.save(message.getTxNo(), String.valueOf(message.getOrderId()), KafkaTopics.TX_PAYMENT_RESULT_TOPIC, payload);
        try {
            kafkaTemplate.send(KafkaTopics.TX_PAYMENT_RESULT_TOPIC, message.getTxNo(), payload);
            txMessageService.markSent(message.getTxNo(), KafkaTopics.TX_PAYMENT_RESULT_TOPIC);
        } catch (Exception e) {
            txMessageService.markFailed(message.getTxNo(), KafkaTopics.TX_PAYMENT_RESULT_TOPIC, e.getMessage());
            throw new BusinessException("支付结果消息发送失败");
        }
    }

    private TxPaymentMessage readValue(String payload) {
        try {
            return objectMapper.readValue(payload, TxPaymentMessage.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("消息解析失败");
        }
    }

    private String writeValue(TxPaymentMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new BusinessException("消息序列化失败");
        }
    }
}
