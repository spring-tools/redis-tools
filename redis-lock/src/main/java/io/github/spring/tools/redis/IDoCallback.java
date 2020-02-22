package io.github.spring.tools.redis;

/**
 * callback 函数
 * @param <T> 返回值类型
 * @author Fenghu.Shi
 * @version 1.0.0
 */
@FunctionalInterface
public interface IDoCallback<T> {


  /**
   * 执行函数
   * @return 执行结果
   * @throws Throwable 异常
   */
  T callback() throws Throwable;

}
