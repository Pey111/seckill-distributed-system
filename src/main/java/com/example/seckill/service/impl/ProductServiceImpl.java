package com.example.seckill.service.impl;

import com.example.seckill.common.BusinessException;
import com.example.seckill.dto.ProductDetailResponse;
import com.example.seckill.dto.ProductSearchResponse;
import com.example.seckill.entity.Product;
import com.example.seckill.mapper.StockMapper;
import com.example.seckill.entity.Stock;
import com.example.seckill.service.ProductQueryService;
import com.example.seckill.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String NULL_VALUE = "__NULL__";
    private static final String PRODUCT_KEY_PREFIX = "product:detail:";
    private static final String LOCK_KEY_PREFIX = "product:lock:";

    private final StockMapper stockMapper;
    private final ProductQueryService productQueryService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public ProductServiceImpl(StockMapper stockMapper,
                              ProductQueryService productQueryService,
                              StringRedisTemplate stringRedisTemplate,
                              ObjectMapper objectMapper) {
        this.stockMapper = stockMapper;
        this.productQueryService = productQueryService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProductDetailResponse getProductDetail(Long productId) {
        String key = PRODUCT_KEY_PREFIX + productId;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (NULL_VALUE.equals(cached)) {
            throw new BusinessException("商品不存在");
        }
        if (cached != null && !cached.isBlank()) {
            return readValue(cached);
        }
        String lockKey = LOCK_KEY_PREFIX + productId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        if (Boolean.TRUE.equals(locked)) {
            try {
                ProductDetailResponse data = loadFromDb(productId);
                if (data == null) {
                    stringRedisTemplate.opsForValue().set(key, NULL_VALUE, 120, TimeUnit.SECONDS);
                    throw new BusinessException("商品不存在");
                }
                stringRedisTemplate.opsForValue().set(key, writeValue(data), ttlSeconds(), TimeUnit.SECONDS);
                return data;
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String retry = stringRedisTemplate.opsForValue().get(key);
        if (NULL_VALUE.equals(retry)) {
            throw new BusinessException("商品不存在");
        }
        if (retry != null && !retry.isBlank()) {
            return readValue(retry);
        }
        ProductDetailResponse data = loadFromDb(productId);
        if (data == null) {
            throw new BusinessException("商品不存在");
        }
        return data;
    }

    @Override
    public List<ProductSearchResponse> search(String keyword) {
        List<Product> products = productQueryService.search(keyword);
        List<ProductSearchResponse> result = new ArrayList<>();
        for (Product product : products) {
            ProductSearchResponse item = new ProductSearchResponse();
            item.setId(product.getId());
            item.setName(product.getName());
            item.setPrice(product.getPrice());
            item.setDescription(product.getDescription());
            result.add(item);
        }
        return result;
    }

    @Override
    public void clearProductCache(Long productId) {
        stringRedisTemplate.delete(PRODUCT_KEY_PREFIX + productId);
    }

    private ProductDetailResponse loadFromDb(Long productId) {
        Product product = productQueryService.getById(productId);
        if (product == null) {
            return null;
        }
        ProductDetailResponse response = new ProductDetailResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setPrice(product.getPrice());
        response.setDescription(product.getDescription());
        response.setStockCount(loadStock(productId));
        return response;
    }

    private ProductDetailResponse readValue(String value) {
        try {
            return objectMapper.readValue(value, ProductDetailResponse.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("缓存读取失败");
        }
    }

    private String writeValue(ProductDetailResponse value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("缓存写入失败");
        }
    }

    private long ttlSeconds() {
        return 300 + random.nextInt(180);
    }

    private Integer loadStock(Long productId) {
        String key = "seckill:stock:" + productId;
        String stock = stringRedisTemplate.opsForValue().get(key);
        if (stock != null) {
            return Integer.parseInt(stock);
        }
        Stock dbRecord = stockMapper.selectById(productId);
        Integer dbStock = dbRecord == null ? 0 : dbRecord.getStockCount();
        stringRedisTemplate.opsForValue().set(key, String.valueOf(dbStock));
        return dbStock;
    }
}
