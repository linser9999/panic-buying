package com.linser;

import com.linser.entity.Shop;
import com.linser.service.IShopService;
import com.linser.service.IShopTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.linser.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class PanicBuyingApplicationTests {

    @Autowired
    private IShopService shopService;

    @Test
    void contextLoads() {
        String key = CACHE_SHOP_KEY + 1;
        shopService.yuReHotKey(key, 1l);
    }

}
