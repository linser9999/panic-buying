package com.linser;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import com.linser.entity.RedisOrder;
import com.linser.entity.Shop;
import com.linser.service.IShopService;
import com.linser.service.IShopTypeService;
import com.linser.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static com.linser.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.linser.utils.RedisConstants.SECKILL_STOCK_KEY;

@SpringBootTest
class PanicBuyingApplicationTests {

    @Autowired
    private IShopService shopService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void redisVoucherTest() {
        String key = SECKILL_STOCK_KEY + 16;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        RedisOrder redisOrder = BeanUtil.toBean(entries, RedisOrder.class);
        System.out.println(redisOrder);

    }

    @Test
    void yuReHotKey() {
        String key = CACHE_SHOP_KEY + 1;
        shopService.yuReHotKey(key, 1l);
    }

    @Test
    void ShiJianCuo() {
        LocalDateTime localDateTime = LocalDateTime.of(2026, 4, 18, 13, 0);
        long epochSecond = localDateTime.toEpochSecond(ZoneOffset.UTC);
        System.out.println(epochSecond);
    }

    @Test
    void getId() {
        Long id = redisIdWorker.getId("shop");
        System.out.println(id);
    }

}
