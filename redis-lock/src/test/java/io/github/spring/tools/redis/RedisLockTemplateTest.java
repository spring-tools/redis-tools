package io.github.spring.tools.redis;

import io.github.spring.tools.redis.exception.TimeoutLockException;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 测试 redis lock template
 * <p>测试 redis lock template</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public class RedisLockTemplateTest extends AbsLockTest {



    @Test
    public void testTryLock() {
        try {
            Assert.assertEquals("张三", RedisLockBuilder.builder(LOCK_KEY).build().execute(() -> {
                // 获取锁后的业务
                return "张三";
            }));
        } catch (TimeoutLockException timeout) {
            // 这里是获取锁失败
        }catch (Throwable throwable) {
           // 这里是 其他业务异常
        }
    }

    @Test
    public void testTryTimeLock() {
        try {
            Assert.assertEquals("张三", RedisLockBuilder.builder(LOCK_KEY).build().execute(() -> "张三", 20, TimeUnit.SECONDS));
        } catch (TimeoutLockException timeout) {
            // 这里是获取锁失败
        }catch (Throwable throwable) {
            // 这里是 其他业务异常
        }
    }

    /**
     * 降级demo
     */
    @Test
    public void testTryFaultLock() throws Exception {
        try(IRedisLock lock = RedisLockBuilder.builder(LOCK_KEY).lockSeconds(10).build();){
            assertTrue(lock.tryLock());
            async(() -> {
                try {
                    Assert.assertEquals("失败了", RedisLockBuilder.builder(LOCK_KEY).build().execute(() -> {
                        // 成功执行
                        return "成功了";
                    }, () -> {
                        // 失败降级处理
                        return "失败了";
                    }));
                } catch (TimeoutLockException timeout) {
                    // 这里是获取锁失败
                }catch (Throwable throwable) {
                    // 这里是 其他业务异常
                }
                return null;
            });
        }

    }

}
