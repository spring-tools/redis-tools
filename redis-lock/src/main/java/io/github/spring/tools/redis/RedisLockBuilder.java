package io.github.spring.tools.redis;

import io.github.spring.tools.redis.decorator.AbsLockDecorator;
import lombok.Getter;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;

import static io.github.spring.tools.redis.capable.ILockWritable.*;

/**
 * 共享锁构造器
 *
 * @author Fenghu.Shi
 * @version 1.2.0
 */
@Getter
public class RedisLockBuilder {

  /**
   * 锁定的 key
   */
  private String key;

  /**
   * 资源锁定时长, 单位秒
   */
  private int lockSeconds = DEFAULT_EMPTY;

  /**
   * 休眠最小时间
   */
  private int sleepMinMills = DEFAULT_EMPTY;

  /**
   * 休眠最大时间
   */
  private int sleepMaxMills = DEFAULT_EMPTY;

  /**
   * 默认自旋次数
   */
  private int spinTimes = DEFAULT_EMPTY;

  /**
   * 回滚时抛出异常
   */
  private RuntimeException throwableException;

  /**
   * 装饰者
   */
  private Set<Class<? extends AbsLockDecorator>> decorators = new HashSet<>(8);


  /**
   * 设置 共享锁 key
   * @param key key
   * @return chain 对象
   */
  public RedisLockBuilder key(String key){
    Objects.requireNonNull(key);
    this.key = key;
    return this;
  }


  /**
   * 设置资源锁定时间，单位秒
   * @param lockSeconds 锁定时长
   * @return chain 对象
   */
  public RedisLockBuilder lockSeconds(int lockSeconds){
    this.lockSeconds = lockSeconds;
    return this;
  }

  /**
   * 设置失败时抛出异常
   * @param throwableException rollback 失败时抛出的异常
   * @return chain 对象
   */
  public RedisLockBuilder throwableException(RuntimeException throwableException){
    this.throwableException = throwableException;
    return this;
  }

  /**
   * 最小休眠时间
   * @param sleepMinMills 最小休眠时间，单位毫秒
   * @return chain
   */
  public RedisLockBuilder sleepMinMills(int sleepMinMills){
    this.sleepMinMills = sleepMinMills;
    return this;
  }

  /**
   * 设置最大休闲时间
   * @param sleepMaxMills 最大休眠时间，单位毫秒
   * @return chain
   */
  public RedisLockBuilder sleepMaxMills(int sleepMaxMills){
    this.sleepMaxMills = sleepMaxMills;
    return this;
  }

  /**
   * 设置 自旋次数
   * @param spinTimes 次数
   * @return chain
   */
  public RedisLockBuilder spinTimes(int spinTimes){
    this.spinTimes = spinTimes;
    return this;
  }

  /**
   * 添加多个装饰者
   * @param classes 装饰者类
   * @return chain 对象
   */
  public RedisLockBuilder addDecorators(Class<? extends AbsLockDecorator>... classes){
    if (classes != null){
      decorators.addAll(Arrays.asList(classes));
    }
    return this;
  }

  /**
   * 添加多个装饰者
   * @param classes 装饰者类
   * @return chain 对象
   */
  public RedisLockBuilder addDecorators(List<Class<? extends AbsLockDecorator>> classes){
    if (classes != null){
      decorators.addAll(classes);
    }
    return this;
  }

  /**
   * 开始生成
   * @return RedisLock
   */
  public RedisLock build(){
    DefaultRedisLock lock = new DefaultRedisLock(
            RedisLockEnvironment.getInstance().getRedisTemplate(),
            key,
            getDefaultValue(lockSeconds, DEFAULT_LOCK_SECONDS),
            getDefaultValue(sleepMinMills, DEFAULT_SLEEP_MIN_MILLS),
            getDefaultValue(sleepMaxMills, DEFAULT_SLEEP_MAX_MILLS),
            getDefaultValue(spinTimes, DEFAULT_SPIN_TIME),
            throwableException
            );
    return RedislockUtils.mergeEnv(lock, decorators);
  }


  /**
   * 获取默认值
   * @param value 要设置的值
   * @param defaultValue 默认值
   * @return 最终值
   */
  private int getDefaultValue(int value, int defaultValue){
    return value == DEFAULT_EMPTY ? defaultValue : value;
  }

  /**
   * 是否默认值
   * @param value 检查的值
   * @return 结果
   */
  boolean isDefault(int value){
    return value == DEFAULT_EMPTY;
  }

  /**
   * 生成 一个 builder
   * @return builder 对象
   */
  public static RedisLockBuilder builder(){
    return builder(null);
  }

  /**
   * 设置 全局环境
   * @param redisTemplate 设置 环境
   * @return 环境对象
   */
  public RedisLockEnvironment buildEnv(RedisTemplate redisTemplate){
    return new RedisLockEnvironment(redisTemplate, this);
  }

  /**
   * 生成
   * @param key key
   * @return 生成一个 builder
   */
  public static RedisLockBuilder builder(String key){
    return new RedisLockBuilder().key(key);
  }


  /**
   * 默认空数据
   */
  public static final int DEFAULT_EMPTY = -1;
}
