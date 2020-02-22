package io.github.spring.tools.redis.annotation;

import io.github.spring.tools.redis.RedisLock;
import io.github.spring.tools.redis.RedisLockBuilder;
import io.github.spring.tools.redis.exception.TimeoutLockException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 将  用于处理 含有 {@link RedisLocks} 注解的方法
 * @author Fenghu.Shi
 * @version 1.0
 * @see io.github.spring.tools.redis.RedisLockBuilder
 */
public final class AnnotationProcess {


  /**
   * bean 容器
   */
   private BeanFactory beanFactory;

   @Autowired
   public AnnotationProcess(BeanFactory beanFactory){
     Objects.requireNonNull(beanFactory);
      this.beanFactory = beanFactory;
   }

  /**
   * 执行
   * @param jp 切入点
   * @return 执行后的数据
   * @throws Throwable 异常
   */
  public final Object handle(ProceedingJoinPoint jp) throws Throwable {
    MethodSignature signature = (MethodSignature) jp.getSignature();
    // 解析 ShareLock 注解
    RedisLocks shareLockAnnotation = signature.getMethod().getAnnotation(RedisLocks.class);
    // 如果不存在，则直接执行
    if (shareLockAnnotation == null) {
      return jp.proceed();
    }
    // 处理
    return lockExecute(jp, shareLockAnnotation);
  }


  /**
   * 锁执行
   * @param jp 切入点
   * @param shareLockAnnotation 注解实例
   * @return 执行结果
   * @throws Throwable  其他异常
   */
  private Object lockExecute(ProceedingJoinPoint jp, RedisLocks shareLockAnnotation) throws Throwable  {
    MethodSignature signature = (MethodSignature) jp.getSignature();
    // 解析参数
    String[] parameterNames = signature.getParameterNames();
    Map<String,Object> params = new HashMap<>(8);
    Object[] args = jp.getArgs();
    if (parameterNames != null && parameterNames.length > 0){
      for (int i = 0;i < parameterNames.length;i ++) {
        params.put(parameterNames[i], args[i]);
      }
    }

    // 获取锁对象
    RedisLock lockObject = buildLockObject(shareLockAnnotation, jp.getTarget(), signature.getMethod(), jp.getArgs(), params);
    //执行结果
    Object processResultObject;
    try{
      boolean lockResult;
      if (shareLockAnnotation.waitTimeoutMills() <= 0){
        lockResult = lockObject.tryLock();
      }else {
        lockResult = lockObject.tryLock(shareLockAnnotation.waitTimeoutMills(), TimeUnit.MILLISECONDS);
      }
      // 处理关于回退
      processResultObject = doExecute(lockObject, jp, shareLockAnnotation);
      // 如果获取锁成功
      if (lockResult) {
        processResultObject = jp.proceed();
      }
    } finally{
      lockObject.unlock();
    }
    Object rollbackData;
    // 是否需要回滚
    if (lockObject.isRollback() && (rollbackData = rollback(jp, shareLockAnnotation)) != NOOP){
        processResultObject = rollbackData;
    }
    return processResultObject;
  }

  /**
   * 根据 锁的状态执行
   * @param lock 所持有对象
   * @param jp 执行点
   * @param shareLockAnnotation 注解
   * @return 直接结果
   * @throws Throwable 异常
   */
  private Object doExecute(RedisLock lock, ProceedingJoinPoint jp, RedisLocks shareLockAnnotation) throws Throwable{
    MethodSignature signature = (MethodSignature) jp.getSignature();
    // 如果获取所成功或者失败继续执行
    if (lock.isLocked()) {
      return jp.proceed();
    }
    // 开始处理失败情况
    // 计算失败规则
    FaultPolicy policy = shareLockAnnotation.faultPolicy() == FaultPolicy.AUTO && !RedisLocks.DEFAULT_METHOD.equals(shareLockAnnotation.fallbackMethod()) ? FaultPolicy.REPLACE : shareLockAnnotation.faultPolicy();
    // 1、啥也不做
    if (policy == FaultPolicy.DO_NOTHING){
      return faultDoNothing(signature, shareLockAnnotation);
    }
    // 2、继续
    if (policy == FaultPolicy.CONTINUE){
      return jp.proceed();
    }
    // 3、回退
    if (policy == FaultPolicy.REPLACE) {
      return faultCallback(jp, shareLockAnnotation.fallbackMethod());
    }
    // 4、抛出异常
    if (policy == FaultPolicy.THROWABLE){
      throw new TimeoutLockException(lock.getKey());
    }
    return NOOP;
  }

  /**
   * 啥也不执行时的回调 这里特别注意，如果返回值类型是 基本类型且非包装器类型
   * @param signature 方法签名
   * @param shareLockAnnotation 注解
   * @return 执行结果
   */
  private Object faultDoNothing(MethodSignature signature, RedisLocks shareLockAnnotation){
    return signature.getReturnType().isPrimitive() && Number.class.isAssignableFrom(signature.getReturnType()) ? RedisLocks.FAULT_NUMBER_DEFAULT : null;
  }

  /**
   * 回退处理
   * @param jp 切入点
   * @param methodName 降级方法名称
   * @return 执行结果
   */
  private Object faultCallback(ProceedingJoinPoint jp, String methodName)
      throws InvocationTargetException, IllegalAccessException {
    MethodSignature signature = (MethodSignature) jp.getSignature();
    // 获取回退方法
    if (StringUtils.isEmpty(methodName)) {
      throw new UnsupportedOperationException(String.format("%s 方法的SharedLock注解未定义 faultMethod 属性", signature.getMethod()));
    }
    try{
      // 获取方法
      Method method = jp.getTarget().getClass().getMethod(methodName, signature.getParameterTypes());
      // 回退方法
      if (!signature.getReturnType().isAssignableFrom(method.getReturnType())) {
        throw new UnsupportedOperationException(String.format("%s 方法的SharedLock注解定义 faultMethod=%s 方法签名错误", signature.getMethod(), methodName));
      }
      // 执行回退
      return method.invoke(jp.getTarget(), jp.getArgs());
    }catch (NoSuchMethodException ex) {
      throw new UnsupportedOperationException(String.format("%s 方法的SharedLock注解定义 faultMethod=%s 方法签名错误", signature.getMethod(), methodName));
    }
  }


  /**
   * 执行回滚
   * @param jp 切入点
   * @param shareLockAnnotation 锁注解
   */
  private Object rollback(ProceedingJoinPoint jp, RedisLocks shareLockAnnotation)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    MethodSignature signature = (MethodSignature) jp.getSignature();
    Method method = jp.getTarget().getClass().getMethod(shareLockAnnotation.rollbackMethod(), signature.getParameterTypes());
    if (method.getReturnType() != null && method.getReturnType().isAssignableFrom(((MethodSignature) jp.getSignature()).getReturnType())){
      return method.invoke(jp.getTarget(), jp.getArgs());
    } else {
      return NOOP;
    }
  }

  /**
   * 将 注解 转换成 lock 对象
   * @param lock 注解实例
   * @param target 调用的目标对象
   * @param method 方法
   * @param args 原始参数
   * @param argMaps 组合参数
   * @return 锁对象
   */
  private RedisLock buildLockObject(RedisLocks lock, Object target, Method method, Object[] args, Map<String, Object> argMaps){
    RedisLockBuilder builder = RedisLockBuilder.builder(buildKey(lock, target, method, argMaps));
    // 锁定时间
    if (lock.lockedSeconds() != RedisLocks.DEFAULT_INT) {
      builder.lockSeconds(lock.lockedSeconds());
    }
    return builder.build();
  }

  /**
   * 生成 key
   * @param lock 锁对象
   * @param target 调用方法的目标对象
   * @param method 执行方法
   * @param args 参数
   * @return key
   */
  private String buildKey(RedisLocks lock, Object target, Method method, Map<String, Object> args){
    String key = StringUtils.isEmpty(lock.key()) ? lock.value() : lock.key();
    // 解析 key
    // 如果是默认的，则直接生成
    if (RedisLocks.DEFAULT_METHOD.equals(key)){
      return method.toString();
    }
    // 生成
    return parseKeyVariables(key, args);
  }


  /**
   * 解析 key 中的 变量
   * @param key 锁的key
   * @param args key的参数
   * @return 结果
   */
  private static String parseKeyVariables(String key, Map<String, Object> args){
    if (args == null || args.isEmpty()){
      return key;
    }
    // 开始逐个参数处理
    for (Map.Entry<String, Object> arg : args.entrySet()) {
      key = key.replaceAll(String.format("\\#\\{%s\\}", arg.getKey()), arg.getValue() == null ? "" : arg.getValue().toString());
    }
    return key;
  }


  private static final Object NOOP = new Object();
}
