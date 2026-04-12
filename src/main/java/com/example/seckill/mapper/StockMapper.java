package com.example.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckill.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Update("update stock set stock_count = stock_count - #{amount} where product_id = #{productId} and stock_count >= #{amount}")
    int reduceStock(@Param("productId") Long productId, @Param("amount") Integer amount);
}
