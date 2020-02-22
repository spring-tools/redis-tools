package io.github.spring.tools.redis.annotation;

/**
 * 失败处理策略
 *
 * @author Fenghu.Shi
 * @version 1.0
 */
public enum FaultPolicy {

  /**
   * 替代，需要设置 {@link RedisLocks#fallbackMethod()} 方法
   */
  REPLACE,

  /**
   * 啥也不做，直接忽略，会返回 NULL
   */
  DO_NOTHING,

  /**
   * 抛出异常 抛出 {@link net.madtiger.lock.exception.TimeoutSharedLockException}
   */
  THROWABLE,

  /**
   * 就算获取不到也继续执行
   */
  CONTINUE,

  /**
   * 自动处理, 如果 {@link RedisLocks#fallbackMethod()} 设置了，则走 回退，否则 {@link RedisLocks#throwable()} 抛出异常
   */
  AUTO


}
