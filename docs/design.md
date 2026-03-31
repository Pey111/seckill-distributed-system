秒杀系统设计文档



系统架构：

用户服务 ：负责用户鉴权、登录与注册。

商品服务 ：维护秒杀商品信息、价格及详情。

订单服务 ：处理下单逻辑及订单状态流转。

库存服务 ：高并发下的库存校验与扣减。



各服务 API 接口定义 (RESTful)

模块           接口地址           请求方式         功能描述

用户      /api/user/login        POST          用户登录认证

商品      /api/product/{id}      GET         获取指定商品详情

订单      /api/order/create     POST        创建秒杀订单

库存      /api/stock/reduce     POST        扣减商品库存



数据库 ER 图表结构：

用户表 (user): id, username, password, phone

商品表 (product): id, name, price, description

库存表 (stock): product\_id, stock\_count

订单表 (order): id, user\_id, product\_id, create\_time, status



技术栈选型说明

编程语言: Java 

基础框架: Spring Boot 3.x

持久层框架: MyBatis Plus

数据库: MySQL 8.0

中间件: Redis 

