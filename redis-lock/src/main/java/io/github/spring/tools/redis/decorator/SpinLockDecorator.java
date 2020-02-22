package io.github.spring.tools.redis.decorator;

import io.github.spring.tools.redis.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.github.spring.tools.redis.RedisLockStatus.*;

/**
 * 支持自旋的锁，一般用于需要多次主动拉取的服务，比如 Redis
 *
 * @author Fenghu.Shi
 * @version 1.2.0
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpinLockDecorator extends AbsLockDecorator {

  /**
   * 构造一个 包装器实例
   *
   * @param delegate 实际执行者
   */
  public SpinLockDecorator(RedisLock delegate) {
    super(delegate);
  }


  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    Objects.requireNonNull(unit);
    // 检查状态
    if (getStatus() != NEW) {
      throw new InterruptedException(String.format("%s 锁的当前状态是 %s 不能再次获取锁", getKey(), getStatus()));
    }
    // 获取成功
    try{
      if (doAcquire(time, unit)) {
        setStatus(LOCKED);
        return true;
      }else {
        // 如果 是 新的，则设置为 长时间
        if (getStatus() == NEW) {
          setStatus(TIMEOUT);
        }
        return false;
      }
    }catch (TimeoutException ex){
      setStatus(TIMEOUT);
      return false;
    }
  }

  @Override
  protected Logger getLogger() {
    return log;
  }


  /**
   * 支持自旋的 获取
   * @param time
   * @param unit
   * @return
   * @throws InterruptedException
   */
  private boolean doAcquire(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
    long timeout = System.currentTimeMillis() + unit.toMillis(time);
    int times;
    int timesCount = 0;
    do {
      times = SPIN_TIMES;
      // 自旋 times 次
      for (; ;) {
        checkTimeout(timeout);
        // 如果获取成功则返回成功
        if (tryLock()){
          debugMessage(String.format(" %s次获取成功，自旋 %s 次", timesCount, SPIN_TIMES - times + 1));
          return true;
        }
        if (--times <= 0){
          break;
        }
        timesCount ++;
        debugMessage(String.format(" %s次获取失败，自旋 %s 次", timesCount, SPIN_TIMES - times));
      }
      // 随机休眠
      try {
        debugMessage(String.format(" %s次获取失败，自旋失败，开始随机休眠", timesCount));
        Thread.sleep(MIN_SLEEP_MILLS + (long) ((MAX_SLEEP_MILLS - MIN_SLEEP_MILLS) * Math.random()));
      } catch (Exception e) {
        return  false;
      }
    } while (true);
  }

  /**
   * 检查 超时
   * @param timeout 检查的时间
   * @throws TimeoutException 超时异常
   */
  private void checkTimeout(long timeout) throws TimeoutException {
    if (System.currentTimeMillis() >= timeout){
      throw new TimeoutException(getKey());
    }
  }

  /**
   * 自旋次数
   */
  private static final int SPIN_TIMES = 3;

  /**
   * 最小休眠时间
   */
  private static final int MIN_SLEEP_MILLS = 100;

  /**
   * 最大休眠时间
   */
  private static final int MAX_SLEEP_MILLS = 500;
}
