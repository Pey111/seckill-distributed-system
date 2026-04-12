package com.example.seckill.config.kafka;

public class KafkaTopics {

    public static final String SECKILL_ORDER_TOPIC = "seckill-order-topic";
    public static final String TX_ORDER_CREATE_TOPIC = "tx-order-create-topic";
    public static final String TX_STOCK_DEDUCT_TOPIC = "tx-stock-deduct-topic";
    public static final String TX_ORDER_RESULT_TOPIC = "tx-order-result-topic";
    public static final String TX_PAYMENT_TOPIC = "tx-payment-topic";
    public static final String TX_PAYMENT_RESULT_TOPIC = "tx-payment-result-topic";

    private KafkaTopics() {
    }
}
