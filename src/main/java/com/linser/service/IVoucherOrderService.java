package com.linser.service;

import com.linser.dto.Result;
import com.linser.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author linser
 * @since 2026-04-16
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    Result xiaDanSeckillVoucher(Long voucherId, LocalDateTime now);
}
