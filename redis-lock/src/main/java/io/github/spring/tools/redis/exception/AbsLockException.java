package io.github.spring.tools.redis.exception;

/**
 * 共享锁异常基类
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public class AbsLockException extends Exception{


  /**
   * key
   */
  protected String key;



  /**
   * 构造函数
   * @param key 释放的key
   */
  public AbsLockException(String key) {
    this.key = key;
  }


}
