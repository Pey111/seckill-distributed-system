package com.example.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckill.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    @Select("select * from payment_record where order_id = #{orderId} limit 1")
    PaymentRecord selectByOrderId(@Param("orderId") Long orderId);
}
