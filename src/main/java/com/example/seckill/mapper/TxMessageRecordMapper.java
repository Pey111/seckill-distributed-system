package com.example.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckill.entity.TxMessageRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TxMessageRecordMapper extends BaseMapper<TxMessageRecord> {

    @Select("select * from tx_message_record where tx_no = #{txNo} and topic = #{topic} limit 1")
    TxMessageRecord selectByTxNoAndTopic(@Param("txNo") String txNo, @Param("topic") String topic);

    @Select("select * from tx_message_record where status in (0, 3) and retry_count < max_retry_count order by id asc limit #{size}")
    List<TxMessageRecord> selectRetryable(@Param("size") int size);

    @Select("select * from tx_message_record where business_key = #{businessKey} order by id asc")
    List<TxMessageRecord> selectByBusinessKey(@Param("businessKey") String businessKey);
}
