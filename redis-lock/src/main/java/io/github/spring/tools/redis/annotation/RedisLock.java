package io.github.spring.tools.redis.annotation;

import io.github.spring.tools.redis.exception.NoopLockException;
import io.github.spring.tools.redis.exception.TimeoutLockException;
import io.github.spring.tools.redis.exception.UnLockFailException;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.lang.reflect.Method;

/**
 * 共享锁配置
 * @author fenghu.shi
 * @version 1.0
 * @see io.github.spring.tools.redis.RedisLockBuilder
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RedisLock {

  /**
   * 锁定的key，支持变量，#{paramName1} #{paramName2} #{paramName3}，调用 tostring 方法生成，null=""
   * 默认 使用 {@link Method#toString()} 方法生成
   */
  @AliasFor("key")
  String value() default DEFAULT_METHOD;

  /**
   * key 值
   */
  String key() default DEFAULT_METHOD;

  /**
   * 锁最长可以持有时间，单位秒，默认 是 {@link io.github.spring.tools.redis.capable.ILockWritable#DEFAULT_LOCK_SECONDS}
   */
  int lockedSeconds() default DEFAULT_INT;

  /**
   * 回调函数，该函数必须是当前对象的公共方法，参数也相同，不需要返回值
   */
  String rollbackMethod() default DEFAULT_METHOD;

  /**
   * 失败降级方法，当获取所失败时，替代方法，方法签名必须跟当前方法一致
   */
  String fallbackMethod() default DEFAULT_METHOD;

  /**
   * 获取所失败执行策略，默认是自动，具体见 {@link FaultPolicy#AUTO}
   */
  FaultPolicy faultPolicy() default FaultPolicy.AUTO;

  /**
   * 最长等待时间，单位秒
   * @return
   */
  int waitTimeoutMills() default DEFAULT_INT;

  /**
   * 获取锁失败抛出的异常，如果定义了异常，则默认
   */
  Class<? extends Throwable> faultThrowableException() default TimeoutLockException.class;

  /**
   * 回滚失败抛出的异常，如果不设置，则忽略
   */
  Class<? extends Throwable> rollbackThrowableException() default UnLockFailException.class;

  /**
   * 默认值
   */
  static final int DEFAULT_INT = Integer.MAX_VALUE;


  /**
   * 默认方法
   */
  static final String DEFAULT_METHOD = "__default";


  /**
   * 失败返回的默认值
   */
  static final byte FAULT_NUMBER_DEFAULT = -1;

  /**
   * 只尝试一次
   */
  static final int TRY_ONE = -10;

}
