package com.example.seckill.service;

import com.example.seckill.entity.TxMessageRecord;

import java.util.List;

public interface TxMessageService {

    void save(String txNo, String businessKey, String topic, String payload);

    void markDone(String txNo, String topic);

    void markFailed(String txNo, String topic, String error);

    void markSent(String txNo, String topic);

    List<TxMessageRecord> listByBusinessKey(String businessKey);

    List<TxMessageRecord> listRetryable(int size);
}
