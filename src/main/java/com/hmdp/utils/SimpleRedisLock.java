package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements Ilock{
    private StringRedisTemplate stringRedisTemplate;
    private String name;
    //uuid 防止两个服务器出现同一个线程
    private static final String ID_UNI= UUID.randomUUID().toString()+"-";

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }


    @Override
    public boolean teylock(long timeoutsec) {
        //set lock thread1 NX EX 10
        long id = Thread.currentThread().getId();
        Boolean suc = stringRedisTemplate.opsForValue().
                    setIfAbsent(ID_UNI+"lock:" + name, id + "", timeoutsec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(suc);
    }

    @Override
    public void unlock() {
        stringRedisTemplate.delete(ID_UNI+"lock:"+name);

    }
}
