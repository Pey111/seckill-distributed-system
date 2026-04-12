package com.example.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckill.entity.OrderRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderRecordMapper extends BaseMapper<OrderRecord> {

    @Select("select count(1) from order_record where user_id = #{userId} and product_id = #{productId}")
    int countByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Select("select * from order_record where user_id = #{userId} order by create_time desc")
    List<OrderRecord> selectByUserId(@Param("userId") Long userId);
}
