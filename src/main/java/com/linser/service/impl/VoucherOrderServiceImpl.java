package com.linser.service.impl;

import com.linser.dto.Result;
import com.linser.entity.SeckillVoucher;
import com.linser.entity.VoucherOrder;
import com.linser.mapper.VoucherOrderMapper;
import com.linser.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linser.utils.RedisIdWorker;
import com.linser.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.linser.utils.ErrorContants.*;

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

    /**
     * 优惠券下单
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId) {

        // 查优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);

        // 恶意请求，优惠券不存在
        if (seckillVoucher == null) {
            return Result.fail(SECKILL_VOUCHER_IS_NULL);
        }

        // 判断优惠券是否有库存
        Integer stock = seckillVoucher.getStock();

        if (stock < 0) {
            // 库存不足
            return Result.fail(WITHOUT_ANY_SECKILL_VOUCHER);
        }

        // 判断优惠券抢购时间
        LocalDateTime now = LocalDateTime.now();
        if (seckillVoucher.getBeginTime().isAfter(now)) {
            // 抢购活动未开始
            return Result.fail(BEFORE_SECKILL_VOUCHER);
        }
        if (seckillVoucher.getEndTime().isBefore(now)) {
            return Result.fail(AFTER_SECKILL_VOUCHER);
        }


        Long userId = UserHolder.getUser().getId();

        // 先上锁， 再开启事务
        synchronized(userId.toString().intern()) {
            // 获取代理对象
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.xiaDanSeckillVoucher(voucherId, now);
        }

    }

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
