package com.example.seckill.config.db;

public class DbContextHolder {

    private static final ThreadLocal<DbType> HOLDER = new ThreadLocal<>();

    public static void use(DbType dbType) {
        HOLDER.set(dbType);
    }

    public static DbType get() {
        DbType dbType = HOLDER.get();
        return dbType == null ? DbType.MASTER : dbType;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
