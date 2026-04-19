package com.linser.service;

import com.linser.dto.Result;
import com.linser.entity.SeckillVoucher;
import com.linser.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author linser
 * @since 2026-04-16
 */
public interface IVoucherService extends IService<Voucher> {

    void addSeckillVoucher(Voucher voucher);

    Result queryVoucherOfShop(Long shopId);
}
