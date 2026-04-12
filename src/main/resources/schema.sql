drop table if exists tx_message_record;
drop table if exists payment_record;
drop table if exists order_record;
drop table if exists stock;
drop table if exists product;
drop table if exists user_account;

create table user_account (
    id bigint primary key auto_increment,
    username varchar(50) not null unique,
    password varchar(100) not null,
    phone varchar(20) not null
);

create table product (
    id bigint primary key auto_increment,
    name varchar(100) not null,
    price decimal(10,2) not null,
    description text
);

create table stock (
    product_id bigint primary key,
    stock_count int not null,
    constraint fk_stock_product foreign key (product_id) references product(id)
);

create table order_record (
    id bigint primary key,
    user_id bigint not null,
    product_id bigint not null,
    amount int not null,
    status tinyint not null,
    create_time datetime not null,
    key idx_user_id(user_id),
    constraint fk_order_user foreign key (user_id) references user_account(id),
    constraint fk_order_product foreign key (product_id) references product(id)
);

create table payment_record (
    id bigint primary key auto_increment,
    order_id bigint not null unique,
    user_id bigint not null,
    status tinyint not null,
    pay_time datetime null,
    create_time datetime not null,
    update_time datetime not null
);

create table tx_message_record (
    id bigint primary key auto_increment,
    tx_no varchar(64) not null,
    business_key varchar(64) not null,
    topic varchar(100) not null,
    payload text not null,
    status tinyint not null,
    retry_count int not null,
    max_retry_count int not null,
    last_error varchar(255) null,
    create_time datetime not null,
    update_time datetime not null,
    unique key uk_tx_no_topic(tx_no, topic),
    key idx_business_key(business_key)
);
