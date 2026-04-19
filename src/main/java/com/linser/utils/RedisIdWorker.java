package com.linser.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 约定开始时间戳：2024.4.18 13：00
     */
    private static Long BEGIN_TIMESTAMP = 1776517200L;

    /**
     * 序列号的位数
     */
    private static final int COUNT_BITS = 32;

    public Long getId(String preKey) {

        // 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;

        // 生成 redis 自增序列号
        // 获取当前日期，精确到天
        String data = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));

        // 取值
        /**
         * 这里 key 拼接日期原因：
         * 因为时间戳是递增的，所以不同天的时间戳一定不同
         * 不同时间戳，redis 自增序列号不会造成重复
         * 且有利于统计每日订单量 以及 防止 redis自增量过大
         */
        Long count = stringRedisTemplate.opsForValue().increment("irc:" + preKey + ":" + data);

        return timeStamp << COUNT_BITS | count;
    }
}
