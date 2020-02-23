package io.github.spring.tools.redis;

import org.junit.Assert;
import org.junit.Test;

/**
 * 测试类
 * <p>测试lock功能</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public class RedisLockTest  extends AbsLockTest {

    private static final String LOCK_KEY = "---test-lock-key";


    /**
     * 测试普通锁
     */
    @Test
    public void testTryLock(){
        IRedisLock lock = RedisLockBuilder.builder(LOCK_KEY).build();
        try{
            Assert.assertTrue(lock.tryLock());
        }finally{
            lock.unlock();
        }
    }

    /**
     * 测试 execute
     * @throws Throwable
     */
    @Test
    public void testExecute() throws Throwable {
        String value = "获取所成功";
        String str = RedisLockBuilder.builder(LOCK_KEY).build().execute(() -> {
            return value;
        });
        Assert.assertSame(value, str);
    }

    /**
     * 测试 try-with-resource
     */
    @Test
    public void testTryWithResource() throws Exception {
        IRedisLock quote;
        try(IRedisLock lock = RedisLockBuilder.builder(LOCK_KEY).build()){
            Assert.assertTrue(lock.tryLock());
            quote = lock;
        }
        Assert.assertTrue(quote.getReleaseStatus() == RedisLockReleaseStatus.SUCCESS);
    }

    /**
     * 测试 可重入锁
     */
    @Test
    public void testReentrantLock(){
        IRedisLock lock = RedisLockBuilder.builder(LOCK_KEY).build();
        try{
            Assert.assertTrue(lock.tryLock());
            innerLock();
        }finally{
            lock.unlock();
        }
    }

    private void innerLock(){
        IRedisLock lock = RedisLockBuilder.builder(LOCK_KEY).build();
        try{

            Assert.assertTrue(lock.tryLock());
        }finally{
            lock.unlock();
        }
    }

}
