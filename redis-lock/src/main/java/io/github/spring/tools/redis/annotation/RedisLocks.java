package io.github.spring.tools.redis.annotation;

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
public @interface RedisLocks {

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
   * 执行服务提供bean名称，默认根据 类型获取
   */
  String provider() default "";

  /**
   * 最大等待时间，单位毫秒，如果 = 1 则表示只尝试一次
   */
  int waitTimeoutMills() default 500;


  /**
   * 锁最长可以持有时间，单位秒，默认 是 {@link #waitTimeoutMills} 的 4 倍时间
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
