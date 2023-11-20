package com.hmdp.utils;

public interface Ilock {

    boolean teylock(long timeoutsec);

    void unlock();
}
