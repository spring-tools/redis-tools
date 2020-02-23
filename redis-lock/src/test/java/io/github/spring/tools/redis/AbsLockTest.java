package io.github.spring.tools.redis;

import io.github.spring.tools.redis.annotation.EnabledRedisLock;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 测试基类
 * <p>测试基类</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnabledRedisLock
public abstract class AbsLockTest {


    static final String LOCK_KEY = "---test-lock-key";

    /**
     * 执行异步
     * @param supplier 提供者
     */
    protected <T> T async(Supplier<T> supplier){
        AsyncHandler<T> async = new AsyncHandler<>(supplier);
        Thread thread = new Thread(async);
        thread.start();
        try {
            thread.join();
            return async.getReturnData();
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
