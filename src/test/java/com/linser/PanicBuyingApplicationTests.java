package com.linser;

import com.linser.entity.Shop;
import com.linser.service.IShopService;
import com.linser.service.IShopTypeService;
import com.linser.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.linser.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class PanicBuyingApplicationTests {

    @Autowired
    private IShopService shopService;
    @Autowired
    private RedisIdWorker redisIdWorker;

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
