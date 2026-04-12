package com.example.seckill.service.impl;

import com.example.seckill.common.BusinessException;
import com.example.seckill.config.kafka.KafkaTopics;
import com.example.seckill.dto.TxPayRequest;
import com.example.seckill.dto.TxPaymentMessage;
import com.example.seckill.entity.OrderRecord;
import com.example.seckill.entity.PaymentRecord;
import com.example.seckill.mapper.OrderRecordMapper;
import com.example.seckill.mapper.PaymentRecordMapper;
import com.example.seckill.service.PaymentService;
import com.example.seckill.service.TxMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final OrderRecordMapper orderRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final TxMessageService txMessageService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(OrderRecordMapper orderRecordMapper,
                              PaymentRecordMapper paymentRecordMapper,
                              TxMessageService txMessageService,
                              KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper) {
        this.orderRecordMapper = orderRecordMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.txMessageService = txMessageService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> submit(TxPayRequest request) {
        OrderRecord order = orderRecordMapper.selectById(request.orderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(request.userId())) {
            throw new BusinessException("用户与订单不匹配");
        }
        if (order.getStatus() == 2) {
            throw new BusinessException("订单已支付");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("当前订单状态不允许支付");
        }
        PaymentRecord payment = paymentRecordMapper.selectByOrderId(request.orderId());
        if (payment == null) {
            payment = new PaymentRecord();
            payment.setOrderId(request.orderId());
            payment.setUserId(request.userId());
            payment.setStatus(0);
            payment.setCreateTime(LocalDateTime.now());
            payment.setUpdateTime(LocalDateTime.now());
            paymentRecordMapper.insert(payment);
        }
        String txNo = "PAY_" + request.orderId();
        TxPaymentMessage message = new TxPaymentMessage();
        message.setTxNo(txNo);
        message.setOrderId(request.orderId());
        message.setUserId(request.userId());
        txMessageService.save(txNo, String.valueOf(request.orderId()), KafkaTopics.TX_PAYMENT_TOPIC, writeValue(message));
        send(txNo, KafkaTopics.TX_PAYMENT_TOPIC, writeValue(message));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", request.orderId());
        data.put("status", "PAYING");
        data.put("message", "支付请求已发送");
        return data;
    }

    @Override
    public Map<String, Object> getByOrderId(Long orderId) {
        PaymentRecord payment = paymentRecordMapper.selectByOrderId(orderId);
        if (payment == null) {
            throw new BusinessException("支付记录不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", payment.getOrderId());
        data.put("userId", payment.getUserId());
        data.put("status", payment.getStatus());
        data.put("statusText", payment.getStatus() == 1 ? "SUCCESS" : payment.getStatus() == 2 ? "FAILED" : "PENDING");
        data.put("payTime", payment.getPayTime());
        return data;
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

    private String writeValue(TxPaymentMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new BusinessException("支付消息序列化失败");
        }
    }
}
