package io.github.spring.tools.redis.capable;

import io.github.spring.tools.redis.IDoCallback;
import io.github.spring.tools.redis.RedisLockReleaseStatus;
import io.github.spring.tools.redis.RedisLockStatus;
import io.github.spring.tools.redis.exception.TimeoutLockException;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Redis 锁 基础服务接口定义
 * <p>定义了Redis 锁对外暴露的服务接口，支持 try-with-resource</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 * @see Lock
 * @see AutoCloseable
 * @see ILockTemplate
 */
public interface ILock extends Lock, AutoCloseable, ILockTemplate {

    /**
     * 获取所的状态
     * @return 当前状态
     */
    RedisLockStatus getStatus();

    /**
     * 获取 释放 状态
     * @return 当期那状态
     */
    RedisLockReleaseStatus getReleaseStatus();

    /**
     * 获取最小休眠时间，毫秒
     * @return
     */
    int getSleepMinMills();

    /**
     * 获取最大休眠时间
     * @return
     */
    int getSleepMaxMills();

    /**
     * 获取自旋次数
     * @return
     */
    int getSpinTimes();

    /**
     * 获取锁的key
     * @return 当前 key
     */
    String getKey();

    /**
     * 需要锁定的时间
     * @return 当前设置的 时间
     */
    int getLockSeconds();

    /**
     * 流程结束
     * @return 是否结束
     */
    default boolean isFinished() {
        return getReleaseStatus() != RedisLockReleaseStatus.NEW;
    }

    /**
     * 是否 以获取锁
     * @return 是否锁定
     */
    default boolean isLocked(){
        return getStatus() == RedisLockStatus.LOCKED && getReleaseStatus() == RedisLockReleaseStatus.NEW;
    }

    /**
     * 是否需要 回滚, status={@link io.github.spring.tools.redis.RedisLockStatus#LOCKED} && releaseStatus={@link RedisLockReleaseStatus#FAIL} 时返回true
     * @return 结果
     */
    default boolean isRollback(){
        return getStatus() == RedisLockStatus.LOCKED && getReleaseStatus() == RedisLockReleaseStatus.FAIL;
    }

    /**
     * 取消当前锁，只对 NEW 状态可用
     * @return 是否取消成功
     */
    boolean interrupted();

    /**
     * 执行需要锁的函数
     * @param callback 获取锁成功执行的函数
     * @param faultCallback 获取所失败后降级参数，=null 时，获取失败将抛出 SharedLockTimeoutException
     * @param rollback 释放资源失败回退操作
     * @param time 等待时间
     * @param unit 时间单位
     * @param <T> 返回值类型
     * @return 返回数据
     * @throws TimeoutLockException 超时异常
     * @throws Throwable 其他业务异常
     */
    @Override
    default <T> T execute(IDoCallback<T> callback, IDoCallback<T> faultCallback, IDoCallback<T> rollback, int time, TimeUnit unit) throws TimeoutLockException, Throwable {
        Objects.requireNonNull(unit);
        T result;
        try{
            // 获取锁
            if (time <= 0 ? tryLock() : tryLock(time, unit)) {
                // 成功，执行业务
                result = callback.callback();
            }else {
                // 失败的话，如果没有设置降级函数，则抛出 异常
                if (faultCallback == null) {
                    throw new TimeoutLockException(getKey());
                }
                // 降级
                result = faultCallback.callback();
            }
        }finally {
            unlock();
            // 检查是否需要回滚
            if (isRollback() && rollback != null) {
                result = rollback.callback();
            }
        }
        return result;
    }

    /**
     * try-with-resource close
     * @throws Exception 关闭失败异常
     */
    @Override
    default void close() throws Exception {
        unlock();
    }

    /**
     * 此方法不支持
     * @return 条件
     */
    @Override
    default Condition newCondition() {
        throw new UnsupportedOperationException("newCondition 不支持");
    }

    /**
     * 锁定，此方法不支持
     */
    @Override
    default void lock() {
        throw new UnsupportedOperationException("lock  方法不支持");
    }


    /**
     * 可重复线程锁，不支持
     * @throws InterruptedException
     */
    @Override
    default void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException("lockInterruptibly  方法不支持");
    }

}
