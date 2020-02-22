package io.github.spring.tools.redis;

import io.github.spring.tools.redis.decorator.AbsLockDecorator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 共享锁 工具
 * @author Fenghu.Shi
 * @version 1.0
 */
@Slf4j
public class RedislockUtils {

  /**
   * 字符串转换成字符串
   * @param str 要转换的字符串
   * @return byte结果
   */
  public static byte[] stringToBytes(String str){
    try{
      return str == null ? null : str.getBytes(StandardCharsets.UTF_8);
    }catch (Throwable ex){
      throw new IllegalArgumentException(ex);
    }
  }

  /**
   * 装饰 共享锁服务
   * @param sharedLock 要包装的 服务
   * @param classes 装饰者类
   * @return redis 结果
   */
  public static synchronized RedisLock mergeEnv(RedisLock sharedLock, Set<Class<? extends AbsLockDecorator>> classes){
    Objects.requireNonNull(sharedLock);
    RedisLockEnvironment environment = RedisLockEnvironment.getInstance();
    // 合并参数
    environment.merge(sharedLock);
    // 合并处理装饰者类
    return doMergeDecoratorClasses(sharedLock, classes);
  }

  /**
   * 合并 装饰者
   * @param sharedLock 要合并的 基础锁实例
   * @param classes 装饰者
   * @return 结果
   */
  private static RedisLock doMergeDecoratorClasses(RedisLock sharedLock, Set<Class<? extends AbsLockDecorator>> classes){
    Objects.requireNonNull(classes);
    RedisLockEnvironment environment = RedisLockEnvironment.getInstance();

    classes = new HashSet<>(classes);
    classes.addAll(environment.getBuilder().getDecorators());
    if (CollectionUtils.isEmpty(classes)) {
      return sharedLock;
    }
    List<Class<? extends AbsLockDecorator>> decoratorList = new ArrayList<>(classes);
    // 排序
    Collections.sort(decoratorList, DECORATOR_COMPARETOR);
    //逐个安装
    for (Class<? extends AbsLockDecorator>  decoratorClass : decoratorList) {
      try {
        sharedLock = decoratorClass.getConstructor(RedisLock.class).newInstance(sharedLock);
      } catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
    }
    return sharedLock;
  }

  /**
   * 比较器
   */
  private static final Comparator<Class<? extends AbsLockDecorator>> DECORATOR_COMPARETOR = Comparator.comparing( clazz -> {
    Integer order = OrderUtils.getOrder(clazz);
    return order == null ? 1000 : order;
  });
}
