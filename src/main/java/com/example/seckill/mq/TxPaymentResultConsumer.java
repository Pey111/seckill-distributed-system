package com.example.seckill.mq;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seckill.common.BusinessException;
import com.example.seckill.config.kafka.KafkaTopics;
import com.example.seckill.dto.TxPaymentMessage;
import com.example.seckill.entity.OrderRecord;
import com.example.seckill.entity.PaymentRecord;
import com.example.seckill.mapper.OrderRecordMapper;
import com.example.seckill.mapper.PaymentRecordMapper;
import com.example.seckill.service.TxMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class TxPaymentResultConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRecordMapper orderRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final TxMessageService txMessageService;

    public TxPaymentResultConsumer(ObjectMapper objectMapper,
                                   OrderRecordMapper orderRecordMapper,
                                   PaymentRecordMapper paymentRecordMapper,
                                   TxMessageService txMessageService) {
        this.objectMapper = objectMapper;
        this.orderRecordMapper = orderRecordMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.txMessageService = txMessageService;
    }

    @KafkaListener(topics = KafkaTopics.TX_PAYMENT_RESULT_TOPIC)
    @Transactional
    public void consume(String payload) {
        TxPaymentMessage message = readValue(payload);
        OrderRecord order = orderRecordMapper.selectById(message.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        orderRecordMapper.update(null, new LambdaUpdateWrapper<OrderRecord>()
                .eq(OrderRecord::getId, message.getOrderId())
                .set(OrderRecord::getStatus, 2));
        paymentRecordMapper.update(null, new LambdaUpdateWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, message.getOrderId())
                .set(PaymentRecord::getStatus, 1)
                .set(PaymentRecord::getPayTime, LocalDateTime.now())
                .set(PaymentRecord::getUpdateTime, LocalDateTime.now()));
        txMessageService.markDone(message.getTxNo(), KafkaTopics.TX_PAYMENT_RESULT_TOPIC);
    }

    private TxPaymentMessage readValue(String payload) {
        try {
            return objectMapper.readValue(payload, TxPaymentMessage.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("消息解析失败");
        }
    }
}
