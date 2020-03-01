package io.github.spring.tools.redis;

import io.github.spring.tools.redis.concurrent.RedisRateLimiter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * 限流器测试类
 * <p>测试限流器</p>
 *
 * @author Fenghu.Shi
 * @version 1.1.0
 */
public class RateLimiterTest extends AbsLockTest{


    private RedisRateLimiter limiter;

    @Before
    public void init(){
        limiter = RedisRateLimiter.create(LOCK_KEY, 10, 1.0);
    }

    @Test
    public void tryAcquireTest() throws InterruptedException {
        Assert.assertTrue(limiter.tryAcquire(5));
        Thread.sleep(1000);
    }

    @Test
    public void tryAcquireTimeoutTest() throws Exception {
        Assert.assertTrue(limiter.tryAcquire(15, 2, TimeUnit.SECONDS));
        Thread.sleep(1000);
    }

    @Test
    public void tryAcquireTimeoutFailTest() throws Exception {
        Assert.assertFalse(limiter.tryAcquire(31, 2, TimeUnit.SECONDS));
        Thread.sleep(1000);
    }

    @Test
    public void tryZoreInitTest() throws InterruptedException {
        Assert.assertFalse(RedisRateLimiter.create(LOCK_KEY + Math.random(), 10, 1.0, 0).tryAcquire(4));
        Thread.sleep(1000);
    }

    @Test
    public void getAllPermits() throws InterruptedException {
        long permits = limiter.tryGetAllPermits();
        System.out.println("permits is " + permits);
        Assert.assertTrue(permits > 0);
        Thread.sleep(1000);
    }
}
