package com.linser.service.impl;

import cn.hutool.core.collection.ListUtil;
import com.linser.dto.Result;
import com.linser.entity.SeckillVoucher;
import com.linser.entity.VoucherOrder;
import com.linser.mapper.VoucherOrderMapper;
import com.linser.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linser.utils.RedisIdWorker;
import com.linser.utils.UserHolder;
import com.linser.utils.lock.impl.RedisLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.linser.utils.ErrorContants.*;
import static com.linser.utils.RedisConstants.SECKILL_STOCK_KEY;
import static com.linser.utils.RedisConstants.SECKILL_USER_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author linser
 * @since 2026-04-16
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private SeckillVoucherServiceImpl seckillVoucherService;
    @Autowired
    private VoucherOrderServiceImpl voucherOrderService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Lua脚本初始化
    private static DefaultRedisScript<Long> redisScript;
    static {
        redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("order.lua"));
        redisScript.setResultType(Long.class);
    }

    /**
     * 这段代码是对的分布式锁改进
     * 将 优惠券抢购资格判断 以及下单操作 基于redis中进行
     * 再发送给 消息队列
     * 通过异步线程执行消息队列的数据
     */
    /**
     * 优惠券下单
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId) throws InterruptedException {

        Long userId = UserHolder.getUser().getId();


        // 进来就执行 Lua脚本 进行下单操作
        Long resultNum = stringRedisTemplate.execute(
                redisScript,
                ListUtil.toList(SECKILL_STOCK_KEY + voucherId, SECKILL_USER_KEY + voucherId),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(UserHolder.getUser().getId())
        );

        switch (resultNum.toString()) {
            case "1":
                // 该优惠券以被抢完
                return Result.fail(WITHOUT_ANY_SECKILL_VOUCHER);
            case "2":
                // 该优惠券秒杀活动还未开始
                return Result.fail(BEFORE_SECKILL_VOUCHER);
            case "3":
                // 该优惠券秒杀活动已结束
                return Result.fail(AFTER_SECKILL_VOUCHER);
            case "4":
                // 用户已经购买过该优惠券
                return Result.fail(USER_ALREADY_BUY);
            case "5":
                // 该优惠券不存在
                return Result.fail(SECKILL_VOUCHER_IS_NULL);
            default:
                break;
        }

        // 创建订单
        // 生成订单号
        VoucherOrder voucherOrder = new VoucherOrder();
        Long orderId = redisIdWorker.getId("order");
        // 订单id
        voucherOrder.setId(orderId);
        // 用户id
        voucherOrder.setUserId(userId);
        // 代金券id
        voucherOrder.setVoucherId(voucherId);

        // 发送到消息队列
        rabbitTemplate.convertAndSend("seckillVoucher", voucherOrder);

        return Result.ok(orderId);
    }


    /**
     * 下面代码是基于 redisson分布式锁实现一人一单功能， 但是实际查数据还是需要到数据库中查询
     * 且优惠券资格抢购判断和下单优惠全是同一线程串行化处理的
     * 在高并发环境下 吞吐量 qps 还是比较慢
     */
    // /**
    //  * 优惠券下单
    //  * @param voucherId
    //  * @return
    //  */
    // @Override
    // public Result seckillVoucher(Long voucherId) throws InterruptedException {
    //
    //     // 查优惠券
    //     SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
    //
    //     // 恶意请求，优惠券不存在
    //     if (seckillVoucher == null) {
    //         return Result.fail(SECKILL_VOUCHER_IS_NULL);
    //     }
    //
    //     // 判断优惠券是否有库存
    //     Integer stock = seckillVoucher.getStock();
    //
    //     if (stock < 0) {
    //         // 库存不足
    //         return Result.fail(WITHOUT_ANY_SECKILL_VOUCHER);
    //     }
    //
    //     // 判断优惠券抢购时间
    //     LocalDateTime now = LocalDateTime.now();
    //     if (seckillVoucher.getBeginTime().isAfter(now)) {
    //         // 抢购活动未开始
    //         return Result.fail(BEFORE_SECKILL_VOUCHER);
    //     }
    //     if (seckillVoucher.getEndTime().isBefore(now)) {
    //         return Result.fail(AFTER_SECKILL_VOUCHER);
    //     }
    //
    //
    //     Long userId = UserHolder.getUser().getId();
    //
    //     // 先上锁， 再开启事务
    //     // 基于redisson分布式锁
    //     RLock lock = redissonClient.getLock("seckillVoucherOrder");
    //     boolean success = lock.tryLock(1, 12, TimeUnit.SECONDS);
    //     if (!success) {
    //         return Result.fail(USER_ALREADY_BUY);
    //     }
    //     IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
    //     return proxy.xiaDanSeckillVoucher(voucherId, now);
    //
    //
    //     /**
    //      * 使用基于 redis 实现的分布式锁1
    //      */
    //     // 获取锁对象
    //     // RedisLock redisLock = new RedisLock(stringRedisTemplate, "seckillVoucherOrder");
    //     //
    //     // // 尝试获取锁
    //     // boolean success = redisLock.onLock(1200);
    //     //
    //     // if (!success) {
    //     //     // 获取锁失败
    //     //     return Result.fail(USER_ALREADY_BUY);
    //     // }
    //     //
    //     // try {
    //     //     IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
    //     //     return proxy.xiaDanSeckillVoucher(voucherId, now);
    //     // } finally {
    //     //     redisLock.unLock();
    //     // }
    //
    //
    //     /**
    //      * 使用 synchronized 上锁
    //      */
    //     // synchronized(userId.toString().intern()) {
    //     //     // 获取代理对象
    //     //     IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
    //     //     return proxy.xiaDanSeckillVoucher(voucherId, now);
    //     // }
    //
    // }

    /**
     * 下单优惠券
     *
     * @param voucherId
     * @param now
     * @return
     */
    @Transactional
    @Override
    public Result xiaDanSeckillVoucher(Long voucherId, LocalDateTime now) {

        // 用户id
        Long userId = UserHolder.getUser().getId();

        // 判断是否已经下过单
        int count = voucherOrderService.query()
                .eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        if (count > 0) {
            // 用户已经购买过该优惠券
            return Result.fail(USER_ALREADY_BUY);
        }

        // 使用乐观锁减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();

        // 减库存失败
        if (!success) {
            return Result.fail("库存不足");
        }

        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 订单id
        long orderId = redisIdWorker.getId("order");
        voucherOrder.setId(orderId);
        // 用户id
        voucherOrder.setUserId(userId);
        // 代金券id
        voucherOrder.setVoucherId(voucherId);
        // 插入订单
        save(voucherOrder);
        return Result.ok(voucherOrder.getId());

    }
}
