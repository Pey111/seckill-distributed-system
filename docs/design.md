秒杀系统设计文档

系统架构：

用户服务：负责用户注册、登录与基本身份校验。

商品服务：负责商品详情、商品搜索、商品缓存。

订单服务：负责普通下单、秒杀订单查询、订单状态展示。

库存服务：负责库存扣减、Redis 预扣库存、数据库最终一致性校验。

消息队列：Kafka 用于异步削峰，将秒杀请求转换为订单消息。

缓存层：Redis 用于商品详情缓存、秒杀库存、重复下单控制、异步状态查询。

数据库：MySQL 主库负责写入，从库负责商品读取与搜索演示。

接口定义：

POST /api/user/register  用户注册

POST /api/user/login  用户登录

GET /api/product/{id}  查询商品详情

GET /api/product/search?keyword=xx  商品搜索

POST /api/order/create  普通下单

GET /api/order/{id}  按订单 ID 查询

GET /api/order/user/{userId}  按用户 ID 查询订单

POST /api/seckill/submit  提交秒杀请求

GET /api/seckill/status/{orderId}  查询秒杀处理状态

GET /api/system/ping  查看当前命中的应用实例

GET /api/system/db/read  查看读库信息

GET /api/system/db/write  查看写库信息

数据库表：

user_account(id, username, password, phone)

product(id, name, price, description)

stock(product_id, stock_count)

order_record(id, user_id, product_id, amount, status, create_time)

技术栈：

Java 21

Spring Boot 3

MyBatis Plus

MySQL 8.0

Redis 7

Kafka 3

Nginx

Docker Compose

第一次作业：

完成系统设计文档、服务拆分、接口定义、ER 设计、技术栈选型、基础项目初始化、用户注册登录。

第二次作业：

完成 Docker 部署、双应用实例、Nginx 负载均衡、动静分离、Redis 商品缓存，并处理缓存穿透、击穿、雪崩。

第三次作业：

完成 MySQL 读写分离环境、代码路由到主库和从库、商品搜索接口、读写分离演示接口。

第四次作业：

完成 Kafka 异步秒杀下单、Redis 预扣库存、雪花算法订单 ID、按订单和按用户查询订单、同一用户同一商品只能秒杀一次、数据库库存最终不超卖。

第五次作业：

完成基于消息最终一致性的分布式事务演示链路。事务下单时先在 Redis 预扣库存，再发送订单创建消息，由订单侧创建订单、库存侧扣减数据库库存、结果消息回写订单状态；支付时先创建支付请求，再通过消息驱动订单状态更新与支付记录落库。系统增加事务消息表和定时重试机制，用于保证下单与库存扣减、订单支付与订单状态更新的最终一致性。
