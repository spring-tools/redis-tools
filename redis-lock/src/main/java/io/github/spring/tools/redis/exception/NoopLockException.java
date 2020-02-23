package io.github.spring.tools.redis.exception;

/**
 * RedisLock 注解中 空异常
 * <p>RedisLock 注解中 空异常</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public class NoopLockException extends AbsLockException{
    /**
     * 构造函数
     *
     * @param key 释放的key
     */
    public NoopLockException(String key) {
        super(key);
    }
}
