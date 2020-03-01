package io.github.spring.tools.redis;

import com.google.common.util.concurrent.RateLimiter;
import io.github.spring.tools.redis.capable.ILockWritable;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Guava 测试
 * <p>Guava 测试</p>
 *
 * @author Fenghu.Shi
 * @version 1.1.0
 */
public class GuavaTest extends AbsLockTest{


    @Test
    public void testGuava(){
//        long times = ((ILockWritable) RedisLockBuilder.builder("xxxxx").build()).getLockClient().queryRedisNow();
//    System.out.println(Instant.ofEpochMilli(times));
//        RateLimiter limiter = RateLimiter.create(100);
//        Assert.assertTrue(limiter.tryAcquire(100));
    }

}
