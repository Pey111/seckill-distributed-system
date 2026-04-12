# seckill-distributed-system

分布式原理课程作业示例项目，当前仓库已补齐前五次作业的基础实现。

## 本地运行

1. 准备 MySQL 主库、MySQL 从库、Redis、Kafka。
2. 在主库和从库都执行 [schema.sql](/D:/seckill-distributed-system/src/main/resources/schema.sql:1) 和 [data.sql](/D:/seckill-distributed-system/src/main/resources/data.sql:1)。
3. 运行 `mvn spring-boot:run`。

## Docker 运行

在 [docker-compose.yml](/D:/seckill-distributed-system/deploy/docker-compose.yml:1) 所在目录执行：

`docker compose up --build`

启动后访问：

- 前端页面：`http://localhost`
- 商品详情：`http://localhost/api/product/1`
- 商品搜索：`http://localhost/api/product/search?keyword=手机`
- 秒杀提交：`POST http://localhost/api/seckill/submit`
- 秒杀状态：`http://localhost/api/seckill/status/{orderId}`
- 分布式事务下单：`POST http://localhost/api/tx/seckill/submit`
- 分布式事务支付：`POST http://localhost/api/tx/pay`
- 分布式事务详情：`http://localhost/api/tx/order/{orderId}`
- 负载均衡检测：`http://localhost/api/system/ping`
- 读库检测：`http://localhost/api/system/db/read`
- 写库检测：`http://localhost/api/system/db/write`
