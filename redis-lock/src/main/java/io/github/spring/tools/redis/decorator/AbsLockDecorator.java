package io.github.spring.tools.redis.decorator;

import io.github.spring.tools.redis.RedisLock;
import io.github.spring.tools.redis.RedisLockReleaseStatus;
import io.github.spring.tools.redis.RedisLockStatus;
import io.github.spring.tools.redis.capable.ILockWritable;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * 共享锁 包装器
 *
 * @author Fenghu.Shi
 * @version 1.2.0
 */
public abstract class AbsLockDecorator implements RedisLock, ILockWritable {

  /**
   * 处理实例
   */
  protected RedisLock delegate;


  /**
   * 设置 状态
   * @param status
   */
  @Override
  public void setStatus (RedisLockStatus status) {
    // 如果是 装饰者，则交由子类委托
    if (delegate instanceof AbsLockDecorator) {
      ((AbsLockDecorator) delegate).setStatus(status);
    }
    // 如果 是 abs shared lock 则设置
    else if (this.delegate instanceof ILockWritable) {
      ((ILockWritable) delegate).setStatus(status);
    }
    // 如果啥也不是则抛出异常
    else {
      throw new UnsupportedOperationException("当前类型不支持 setStatus 方法");
    }
  }

  /**
   * 构造一个 包装器实例
   * @param delegate 实际执行者
   */
  public AbsLockDecorator(RedisLock delegate){
    this.delegate = delegate;
  }

  @Override
  public RedisLockStatus getStatus() {
    return delegate.getStatus();
  }

  @Override
  public String getKey() {
    return delegate.getKey();
  }

  @Override
  public int getLockSeconds() {
    return delegate.getLockSeconds();
  }

  @Override
  public boolean needUnlock() {
    return ((ILockWritable) delegate).needUnlock();
  }

  @Override
  public boolean isFinished() {
    return delegate.isFinished();
  }

  @Override
  public boolean interrupted() {
    return delegate.interrupted();
  }

  /**
   * 获取特定 class 类型的装饰者实例
   * @param clazz 类名
   * @param <T> 类型
   * @return 结果
   */
  public final <T extends AbsLockDecorator> T getDecoratorByClass(Class<T> clazz){
    // 当前对象是否是 此装饰者实例
    if (clazz.isAssignableFrom(this.getClass())){
      return (T) this;
    }
    // 检查上一级是否是
    if (clazz.isAssignableFrom(this.delegate.getClass())) {
      return (T) this.delegate;
    }
    // 上一级是否是非装饰者
    if (!(this.delegate instanceof AbsLockDecorator)) {
      return null;
    }
    return ((AbsLockDecorator) this.delegate).getDecoratorByClass(clazz);
  }


  /**
   * 获取 logger 对象
   * @return 对象
   */
  protected abstract Logger getLogger();


  /**
   * 来一个 debug 消息
   * @param message 消息
   */
  protected void debugMessage(String message){
    getLogger().debug(String.format("%s:SharedLockDecorator --> %s 锁 %s", getClass().getSimpleName(), getKey(), message));
  }

  /**
   * 来一个 info 消息
   * @param message 消息
   */
  protected void infoMessage(String message){
    getLogger().info(String.format("%s:SharedLockDecorator --> %s 锁 %s", getClass().getSimpleName(), getKey(), message));
  }

  /**
   * 来一个 error 消息
   * @param message 消息
   */
  protected void errorMessage(String message){
    getLogger().error(String.format("%s:SharedLockDecorator --> %s 锁 %s", getClass().getSimpleName(), getKey(), message));
  }

  @Override
  public void setLockSeconds(int lockSeconds) {
    ((ILockWritable) delegate).setLockSeconds(lockSeconds);
  }

  @Override
  public void unlocked(boolean release) {
    ((ILockWritable) delegate).unlocked(release);
  }


  @Override
  public RedisLockReleaseStatus getReleaseStatus() {
    return delegate.getReleaseStatus();
  }

  @Override
  public void setReleaseStatus(RedisLockReleaseStatus releaseStatus) {
    ((ILockWritable) delegate).setReleaseStatus(releaseStatus);
  }

  @Override
  public void setSleepMinMills(int sleepMinMills) {
    ((ILockWritable) delegate).setSleepMinMills(sleepMinMills);
  }

  @Override
  public int getSleepMinMills() {
      return delegate.getSleepMinMills();
  }

  @Override
  public void setSleepMaxMills(int sleepMaxMills) {
    ((ILockWritable) delegate).setSleepMaxMills(sleepMaxMills);
  }

  @Override
  public int getSleepMaxMills() {
    return delegate.getSleepMaxMills();
  }

  @Override
  public void setSpinTimes(int spinTimes) {
    ((ILockWritable) delegate).setSpinTimes(spinTimes);
  }

  @Override
  public int getSpinTimes() {
    return delegate.getSpinTimes();
  }

  @Override
  public boolean tryLock() {
    return delegate.tryLock();
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    return delegate.tryLock(time, unit);
  }

  @Override
  public void unlock() {
    delegate.unlock();
    debugMessage("释放锁成功");
  }
  /**
   * 默认的序号
   */
  public static final int ORDER_DEFAULT = 1000;
}
