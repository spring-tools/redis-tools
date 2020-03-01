package io.github.spring.tools.redis.annotation.configuration;

import io.github.spring.tools.redis.RedisLockBuilder;
import io.github.spring.tools.redis.annotation.RedisLockInterceptor;
import io.github.spring.tools.redis.decorator.AbsLockDecorator;
import io.github.spring.tools.redis.decorator.ReentrantLockDecorator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * 抽象的共享锁配置，将一些功能的方法和bean在这里初始化，所有 configuration 子类必须继承自此类
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public  class RedisLockConfiguration{

  @Autowired
  private RedisTemplate redisTemplate;

  /**
   * 创建 共享锁 拦截器
   * @param context spring 上下文
   * @return interceptor
   */
  @Bean
  public RedisLockInterceptor annotationSharedLoadInterceptor(ApplicationContext context){
    return new RedisLockInterceptor(context);
  }

  /**
   * 初始化 lock
   */
  @PostConstruct
  public void initRedisLock(){
    RedisLockBuilder.builder("").keyPrefix("redis-lock:").addDecorators(defaultDecorators()).buildEnv(redisTemplate);
  }

  /**
   * 获取 默认 服务
   * @return 默认支持的 decorator
   */
  private List<Class<? extends AbsLockDecorator>> defaultDecorators() {
    return Arrays.asList(ReentrantLockDecorator.class);
  }

}
