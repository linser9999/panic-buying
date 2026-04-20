package com.linser.utils.mq;

import com.linser.dto.Result;
import com.linser.entity.VoucherOrder;
import com.linser.service.IVoucherOrderService;
import com.linser.service.impl.SeckillVoucherServiceImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VoucherOrderListener {

    @Autowired
    private IVoucherOrderService voucherOrderService;
    @Autowired
    private SeckillVoucherServiceImpl seckillVoucherService;


    /**
     * 监听消息队列并保存到数据库
     * @param voucherOrder
     */
    @RabbitListener(queues = "seckillVoucher")
     public void insertOrderDatabase(VoucherOrder voucherOrder) {

        // 使用乐观锁减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0)
                .update();


            // 将订单保存到数据库
            voucherOrderService.save(voucherOrder);
     }
}
