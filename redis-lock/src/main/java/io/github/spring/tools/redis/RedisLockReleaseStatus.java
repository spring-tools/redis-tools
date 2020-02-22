package io.github.spring.tools.redis;

/**
 * 锁释放状态
 * <p>定义锁释放结果</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public enum  RedisLockReleaseStatus {

    /**
     * 未释放
     */
    NEW,

    /**
     * 释放成功
     */
    SUCCESS,

    /**
     * 释放失败
     */
    FAIL;

}
