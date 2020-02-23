package io.github.spring.tools.redis;

import io.github.spring.tools.redis.capable.ILockWritable;
import lombok.Getter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 共享锁环境变量
 *
 * @author Fenghu.Shi
 * @version 1.2.0
 */
public class RedisLockEnvironment {

  /**
   * 来个单例对象
   */
  private static RedisLockEnvironment INSTANCE;

  /**
   * 存储全局参数
   */
  @Getter
  private RedisLockBuilder builder;

  /**
   * redis 魔板类
   */
  @Getter
  private RedisTemplate<String, String> redisTemplate;

  /**
   * 构造一个 实例
   * @param redisTemplate redis 客户端
   */
  RedisLockEnvironment(RedisTemplate<String, String> redisTemplate, RedisLockBuilder builder){
    Objects.requireNonNull(redisTemplate);
    Objects.requireNonNull(builder);
    this.builder = builder;
    this.redisTemplate = redisTemplate;
    INSTANCE = this;
  }


  /**
   * 获取 实例
   * @return 环境实例
   */
  public static RedisLockEnvironment getInstance(){
    return INSTANCE;
  }

  /**
   * 获取 已设置的 builder
   * @return
   */
  public static RedisLockBuilder getBuilder(){
    Objects.requireNonNull(INSTANCE);
    return INSTANCE.builder;
  }


  /**
   * 开始设置
   * @param lock 设置的 锁对象
   * @param keyPrefix key 原始的前缀
   */
  void merge(IRedisLock lock, String keyPrefix){
    // 检查是否可设置
    if (lock.getStatus() != RedisLockStatus.NEW){
      throw new IllegalArgumentException(String.format("%s 状态的锁不可以设置参数", lock.getStatus()));
    }
    // 开启设置
    ILockWritable writable = (ILockWritable) lock;
    if (!builder.isDefault(builder.getLockSeconds())){
      writable.setLockSeconds(builder.getLockSeconds());
    }
    if (!builder.isDefault(builder.getSpinTimes())){
      writable.setLockSeconds(builder.getSpinTimes());
    }
    if (!builder.isDefault(builder.getSleepMinMills())){
      writable.setLockSeconds(builder.getSleepMinMills());
    }
    if (!builder.isDefault(builder.getSleepMaxMills())){
      writable.setLockSeconds(builder.getSleepMaxMills());
    }

    // 设置 prefix
    if (!StringUtils.isEmpty(builder.getKeyPrefix()) && StringUtils.isEmpty(keyPrefix)) {
      writable.setKey(String.format("%s-%s", builder.getKeyPrefix(), lock.getKey()));
    }
  }


}
