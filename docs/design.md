秒杀系统设计文档

1. 架构设计
系统采用微服务架构思想，将业务拆分为四个核心服务：
用户服务：处理鉴权与信息管理。
商品服务：管理秒杀商品列表与详情。
订单服务：处理秒杀下单逻辑。
库存服务：负责高并发下的库存扣减。

2. API 接口定义 (RESTful)
POST /api/user/login : 用户登录
GET /api/product/list : 获取秒杀商品列表
POST /api/seckill/execute : 执行秒杀下单

3. 数据库设计
user 表: id, username, password
product 表: id, name, price, stock
order 表: id, user_id, product_id, status