package com.linser.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.linser.dto.Result;
import com.linser.entity.Shop;
import com.linser.mapper.ShopMapper;
import com.linser.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linser.utils.RedisData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.linser.utils.ErrorContants.DATABASE_WITHOUT_DATA;
import static com.linser.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author linser
 * @since 2026-04-16
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    // 线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据id查询商铺信息
     * @param id
     * @return
     */
    @Override
    public Result queryShopById(Long id) {

        Shop shop;

        String key = CACHE_SHOP_KEY + id;

        // 解决缓存穿透
        // shop = huanCunChuangTou(key, id);

        // 互斥锁解决缓存击穿
        // shop = huChiSuo(key, id);

        // 逻辑过期时间解决缓存击穿
        shop = luoJiGuoQI(key, id);

        if (shop == null) {
            return Result.fail(DATABASE_WITHOUT_DATA);
        }

        return Result.ok(shop);
    }

    /**
     * 更新商铺信息
     * @param shop
     * @return
     */
    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        // 更新数据库
        updateById(shop);

        // 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());

        return Result.ok();
    }

    /**
     * 解决缓存穿透问题
     * @param key
     * @param id
     * @return
     */
    private Shop huanCunChuangTou(String key, Long id) {
        Shop shop = null;

        // 查询 redis
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //  redis 中有数据
        if (shopJson != null) {
            // 空值
            if (shopJson.equals(CACHE_NULL_VALUE)) {
                return shop;
            }
            shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        // 缓存重建
        this.huanCunChongJian(key, id);

        return shop;
    }

    /**
     * 互斥锁解决缓存击穿
     * @param key
     * @param id
     * @return
     */
    private Shop huChiSuo(String key, Long id) {
        Shop shop = null;

        // 查询 redis
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //  redis 中有数据
        if (shopJson != null) {
            // 空值
            if (shopJson.equals(CACHE_NULL_VALUE)) {
                return shop;
            }
            shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        // 缓存重建
        String lockKey = LOCK_SHOP_KEY + id;

        try {
            // 尝试获取锁
            boolean isLock = getLock(lockKey);

            // 获取锁失败
            if (!isLock) {
                Thread.sleep(50);
                // 递归实现 查询缓存 或者 获取锁
                return huChiSuo(key, id);
            }

            // 获取锁成功
            // 重新查询缓存, 防止别的线程以及缓存重建完成
            shopJson = stringRedisTemplate.opsForValue().get(key);
            //  redis 中有数据
            if (shopJson != null) {
                // 空值
                if (shopJson.equals(CACHE_NULL_VALUE)) {
                    return shop;
                }
                shop = JSONUtil.toBean(shopJson, Shop.class);
                return shop;
            }

            // 缓存没有重建，说明该线程 获取锁成功并需要缓存重建
            // 缓存重建
            this.huanCunChongJian(key, id);
            // 模拟缓存重建延迟
            Thread.sleep(200);


        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 释放锁
            removeLock(lockKey);
        }

        return shop;
    }

    /**
     * 逻辑过期时间解决缓存击穿
     * @param key
     * @param id
     * @return
     */
    private Shop luoJiGuoQI(String key, Long id) {
        Shop shop = null;

        // 查询缓存
        String redisDataJson = stringRedisTemplate.opsForValue().get(key);

        // 热点key 有问题, 查不到数据
        if (redisDataJson == null || redisDataJson.equals(CACHE_NULL_VALUE)) {
            return shop;
        }

        RedisData redisData = JSONUtil.toBean(redisDataJson, RedisData.class);

        shop = BeanUtil.toBean(redisData.getData(), Shop.class);

        // 获取逻辑过期时间
        LocalDateTime expireTime = redisData.getExpireTime();

        // 没过期, 直接返回 shop
        if (expireTime.isAfter(LocalDateTime.now())) {
            return shop;
        }

        //过期
        // 尝试获取锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = getLock(lockKey);
        // 获取锁失败，返回旧数据
        if (!isLock) {
            return shop;
        }

        // 获取锁成功，
        // 重新查询缓存判断是否有人重建好了
        redisDataJson = stringRedisTemplate.opsForValue().get(key);
        // 热点key 有问题, 查不到数据
        if (redisDataJson == null || redisDataJson.equals(CACHE_NULL_VALUE)) {
            return shop;
        }
        redisData = JSONUtil.toBean(redisDataJson, RedisData.class);
        shop = BeanUtil.toBean(redisData.getData(), Shop.class);
        // 获取逻辑过期时间
        expireTime = redisData.getExpireTime();
        // 没过期, 直接返回 shop
        if (expireTime.isAfter(LocalDateTime.now())) {
            return shop;
        }

        // 没有人重建过
        // 开启新线程 缓存重建
        CACHE_REBUILD_EXECUTOR.submit(() -> {
            try {
                this.yuReHotKey(key, id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                removeLock(lockKey);
            }
        });

        return shop;
    }

    /**
     * 预热 热点key shop信息
     * 缓存重建也可以用这个方法
      * @param key
     * @param id
     */
    @Override
    public void yuReHotKey(String key, Long id) {
        Shop shop = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(10));

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 获取互斥锁
     * @param key
     * @return
     */
    private boolean getLock(String key) {
        Boolean islock = stringRedisTemplate.opsForValue().setIfAbsent(key, "hai!", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(islock);
    }

    /**
     * 释放互斥锁
     * @param key
     */
    private void removeLock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 缓存重建 shop
     * @param key
     * @param id
     */
    private void huanCunChongJian(String key, Long id) {
        // 查询数据库
        Shop shop = getById(id);

        // 数据不存在
        if (shop == null) {
            // 缓存空值
            stringRedisTemplate.opsForValue()
                    .set(key, CACHE_NULL_VALUE, CACHE_NULL_TTL, TimeUnit.MINUTES);
        }

        // 缓存数据
        stringRedisTemplate.opsForValue()
                .set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
    }
}
