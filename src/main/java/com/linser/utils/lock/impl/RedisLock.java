package com.linser.utils.lock.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import com.linser.utils.UserHolder;
import com.linser.utils.lock.Lock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RedisLock implements Lock {

    private final StringRedisTemplate stringRedisTemplate;
    private final String YeWuName;

    public RedisLock(StringRedisTemplate stringRedisTemplate, String yeWuName) {
        this.stringRedisTemplate = stringRedisTemplate;
        YeWuName = yeWuName;
    }

    private static final DefaultRedisScript<Long> defaultRedisScript;
    static {
        defaultRedisScript = new DefaultRedisScript<>();
        defaultRedisScript.setLocation(new ClassPathResource("unlock.lua"));
        defaultRedisScript.setResultType(Long.class);
    }

    private static final String VALUE_PREFIX = UUID.randomUUID().toString(true) + "-";

    private static final String prePreKey = "lock:";

    @Override
    public boolean onLock(long timeOut) {
        String key = prePreKey + YeWuName + ":" + UserHolder.getUser().getId();
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, VALUE_PREFIX + Thread.currentThread().getId(), timeOut, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(success);
    }

    @Override
    public void unLock() {
        String key = prePreKey + YeWuName + ":" + UserHolder.getUser().getId();
        stringRedisTemplate
                .execute(
                        defaultRedisScript,
                        Collections.singletonList(key),
                        VALUE_PREFIX + Thread.currentThread().getId()
                        );
    }
}
