package com.example.seckill.job;

import com.example.seckill.entity.TxMessageRecord;
import com.example.seckill.service.TxMessageService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TxMessageRetryJob {

    private final TxMessageService txMessageService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public TxMessageRetryJob(TxMessageService txMessageService, KafkaTemplate<String, String> kafkaTemplate) {
        this.txMessageService = txMessageService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 20000)
    public void retry() {
        List<TxMessageRecord> records = txMessageService.listRetryable(20);
        for (TxMessageRecord record : records) {
            try {
                kafkaTemplate.send(record.getTopic(), record.getTxNo(), record.getPayload());
                txMessageService.markSent(record.getTxNo(), record.getTopic());
            } catch (Exception e) {
                txMessageService.markFailed(record.getTxNo(), record.getTopic(), e.getMessage());
            }
        }
    }
}
