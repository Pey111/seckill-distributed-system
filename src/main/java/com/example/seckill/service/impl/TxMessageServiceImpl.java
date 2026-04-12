package com.example.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.seckill.entity.TxMessageRecord;
import com.example.seckill.mapper.TxMessageRecordMapper;
import com.example.seckill.service.TxMessageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TxMessageServiceImpl implements TxMessageService {

    private final TxMessageRecordMapper txMessageRecordMapper;

    public TxMessageServiceImpl(TxMessageRecordMapper txMessageRecordMapper) {
        this.txMessageRecordMapper = txMessageRecordMapper;
    }

    @Override
    public void save(String txNo, String businessKey, String topic, String payload) {
        TxMessageRecord old = txMessageRecordMapper.selectByTxNoAndTopic(txNo, topic);
        if (old != null) {
            return;
        }
        TxMessageRecord record = new TxMessageRecord();
        record.setTxNo(txNo);
        record.setBusinessKey(businessKey);
        record.setTopic(topic);
        record.setPayload(payload);
        record.setStatus(0);
        record.setRetryCount(0);
        record.setMaxRetryCount(5);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        txMessageRecordMapper.insert(record);
    }

    @Override
    public void markDone(String txNo, String topic) {
        txMessageRecordMapper.update(null, new LambdaUpdateWrapper<TxMessageRecord>()
                .eq(TxMessageRecord::getTxNo, txNo)
                .eq(TxMessageRecord::getTopic, topic)
                .set(TxMessageRecord::getStatus, 2)
                .set(TxMessageRecord::getUpdateTime, LocalDateTime.now())
                .set(TxMessageRecord::getLastError, null));
    }

    @Override
    public void markFailed(String txNo, String topic, String error) {
        TxMessageRecord record = txMessageRecordMapper.selectByTxNoAndTopic(txNo, topic);
        int retry = record == null || record.getRetryCount() == null ? 0 : record.getRetryCount();
        txMessageRecordMapper.update(null, new LambdaUpdateWrapper<TxMessageRecord>()
                .eq(TxMessageRecord::getTxNo, txNo)
                .eq(TxMessageRecord::getTopic, topic)
                .set(TxMessageRecord::getStatus, 3)
                .set(TxMessageRecord::getRetryCount, retry + 1)
                .set(TxMessageRecord::getLastError, error)
                .set(TxMessageRecord::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public void markSent(String txNo, String topic) {
        txMessageRecordMapper.update(null, new LambdaUpdateWrapper<TxMessageRecord>()
                .eq(TxMessageRecord::getTxNo, txNo)
                .eq(TxMessageRecord::getTopic, topic)
                .set(TxMessageRecord::getStatus, 1)
                .set(TxMessageRecord::getUpdateTime, LocalDateTime.now())
                .set(TxMessageRecord::getLastError, null));
    }

    @Override
    public List<TxMessageRecord> listByBusinessKey(String businessKey) {
        return txMessageRecordMapper.selectByBusinessKey(businessKey);
    }

    @Override
    public List<TxMessageRecord> listRetryable(int size) {
        return txMessageRecordMapper.selectRetryable(size);
    }
}
