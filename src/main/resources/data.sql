insert into user_account(username, password, phone) values
('admin', '4297f44b13955235245b2497399d7a93', '13800138000'),
('alice', '4297f44b13955235245b2497399d7a93', '13800138001'),
('bob', '4297f44b13955235245b2497399d7a93', '13800138002');

insert into product(name, price, description) values
('秒杀手机', 1999.00, '热门手机，适合演示商品搜索、缓存和秒杀流程'),
('机械键盘', 299.00, '第二个演示商品，用于测试缓存和库存'),
('蓝牙耳机', 399.00, '适合测试商品搜索和读写分离');

insert into stock(product_id, stock_count) values
(1, 20),
(2, 50),
(3, 30);
