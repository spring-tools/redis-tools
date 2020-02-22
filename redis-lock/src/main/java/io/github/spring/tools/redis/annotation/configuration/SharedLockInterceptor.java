package io.github.spring.tools.redis.annotation.configuration;

import net.madtiger.lock.AnnotationProcess;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * Shared Lock spring  aop 拦截器，拦截相关的方法，并处理
 * @author Fenghu.Shi
 * @version 1.0
 */
@Aspect
public class SharedLockInterceptor {


  private AnnotationProcess annotationProcess;


  @Autowired
  public SharedLockInterceptor(BeanFactory beanFactory){
    Objects.requireNonNull(beanFactory);
    annotationProcess = new AnnotationProcess(beanFactory);
  }

  /**
   * 环绕执行
   * @param point
   * @return
   * @throws Throwable
   */
  @Around("@annotation(net.madtiger.lock.SharedLock)")
  public Object handle(ProceedingJoinPoint point) throws Throwable{
    return annotationProcess.handle(point);
  }

}
