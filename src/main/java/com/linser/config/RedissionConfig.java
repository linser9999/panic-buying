package com.linser.config;

import com.linser.properties.RedissionProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissionConfig {

    @Bean
    public RedissonClient redissionClient(RedissionProperties redissionProperties) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + redissionProperties.getHost() + ":" + redissionProperties.getPort())
                .setPassword(redissionProperties.getPassword());
        return Redisson.create(config);
    }
}
