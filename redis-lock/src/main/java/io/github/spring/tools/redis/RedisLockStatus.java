package io.github.spring.tools.redis;

/**
 * Redis Lock 的 状态定义枚举
 * <p>定义了Redis Lock 存在的状态</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public enum RedisLockStatus {

    /**
     * 新建
     */
    NEW,

    /**
     * 已锁定
     */
    LOCKED,

    /**
     * 获取所超时
     */
    TIMEOUT,

    /**
     * 取消后解锁
     */
    CANCEL;

}
