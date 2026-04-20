package com.linser.utils.mq;

import com.linser.service.IVoucherOrderService;
import com.linser.service.impl.VoucherOrderServiceImpl;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueuesConfig {

    // 声明队列
    @Bean
    public Queue onlyQueue() {
        return new Queue("seckillVoucher");
    }
}
