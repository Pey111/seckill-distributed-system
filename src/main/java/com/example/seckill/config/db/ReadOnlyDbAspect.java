package com.example.seckill.config.db;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(-1)
public class ReadOnlyDbAspect {

    @Around("@annotation(com.example.seckill.config.db.ReadOnlyDb) || @within(com.example.seckill.config.db.ReadOnlyDb)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DbContextHolder.use(DbType.SLAVE);
            return joinPoint.proceed();
        } finally {
            DbContextHolder.clear();
        }
    }
}
