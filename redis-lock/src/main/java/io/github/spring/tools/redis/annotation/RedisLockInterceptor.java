package io.github.spring.tools.redis.annotation;

import io.github.spring.tools.redis.annotation.AnnotationProcess;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Objects;

/**
 * Shared Lock spring  aop 拦截器，拦截相关的方法，并处理，事务的aop order 默认是{@lnk Ordered#LOWEST_PRECEDENCE},见{@link EnableTransactionManagement#order()}
 * @author Fenghu.Shi
 * @version 1.0.0
 * @see EnableTransactionManagement
 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class RedisLockInterceptor {


  private AnnotationProcess annotationProcess;


  @Autowired
  public RedisLockInterceptor(BeanFactory beanFactory){
    Objects.requireNonNull(beanFactory);
    annotationProcess = new AnnotationProcess(beanFactory);
  }

  /**
   * 环绕执行
   * @param point
   * @return
   * @throws Throwable
   */
  @Around("@annotation(io.github.spring.tools.redis.annotation.RedisLock)")
  public Object handle(ProceedingJoinPoint point) throws Throwable{
    return annotationProcess.handle(point);
  }

}
