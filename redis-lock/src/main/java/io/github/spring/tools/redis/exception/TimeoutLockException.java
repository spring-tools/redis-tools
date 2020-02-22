package io.github.spring.tools.redis.exception;

/**
 * 获取锁超时异常
 * <p>在固定时间段内获取锁超时，抛出此异常</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public class TimeoutLockException extends AbsLockException{

    /**
     * 构造函数
     *
     * @param key 释放的key
     */
    public TimeoutLockException(String key) {
        super(key);
    }
}
