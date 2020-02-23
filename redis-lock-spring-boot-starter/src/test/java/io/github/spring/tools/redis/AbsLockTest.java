package io.github.spring.tools.redis;

import io.github.spring.tools.redis.annotation.EnabledRedisLock;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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
public abstract class AbsLockTest {


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
