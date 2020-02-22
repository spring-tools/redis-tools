package io.github.spring.tools.redis.exception;

/**
 * 解锁失败异常
 * <p>解锁失败时抛出此异常</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public class UnLockFailException extends AbsLockException{

    /**
     * 构造函数
     *
     * @param key 释放的key
     */
    public UnLockFailException(String key) {
        super(key);
    }
}
