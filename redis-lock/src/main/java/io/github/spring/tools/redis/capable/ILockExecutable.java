package io.github.spring.tools.redis.capable;

import io.github.spring.tools.redis.IDoCallback;
import io.github.spring.tools.redis.exception.TimeoutLockException;
import org.springframework.lang.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Redis Lock callback 执行能力定义
 * <p>定义了 redis 执行能力接口规范</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public interface ILockExecutable {

    /**
     * 委托执行，支持降级和回滚，只尝试获取一次，失败立即返回
     * @param callback 获取锁成功执行的函数
     * @param faultCallback 获取所失败后降级参数，=null 时，获取失败将抛出 SharedLockTimeoutException
     * @param rollback 释放资源失败回退操作
     * @param <T> 返回值类型
     * @return 返回值
     * @throws TimeoutLockException 获取锁失败
     * @throws Throwable 其他异常，包括业务异常等
     */
    default <T> T execute(IDoCallback<T> callback, @Nullable IDoCallback<T> faultCallback, @Nullable IDoCallback<T> rollback) throws TimeoutLockException, Throwable {
        return execute(callback, faultCallback, rollback, -1, TimeUnit.SECONDS);
    }


    /**
     * 委托执行，支持降级，只尝试获取一次，失败立即返回
     * @param callback 获取所成功执行的函数
     * @param faultCallback 获取所失败后降级参数，=null 时，获取失败将抛出 LockTimeoutException
     * @param <T> 返回值类型
     * @return 返回值
     * @throws TimeoutLockException  获取锁失败
     * @throws Throwable 其他异常，包括业务异常等
     */
    default <T> T execute(IDoCallback<T> callback, @Nullable IDoCallback<T> faultCallback)  throws TimeoutLockException, Throwable{
        return execute(callback, faultCallback, null);
    }



    /**
     * 委托执行，支持降级和回滚，如果获取所失败，则抛出异常，只尝试获取一次，失败立即返回
     * @param callback 获取锁成功执行的函数
     * @param <T> 返回值类型
     * @return 返回值
     * @throws TimeoutLockException 获取锁失败
     * @throws Throwable 其他异常
     */
    default <T> T execute(IDoCallback<T> callback)  throws TimeoutLockException, Throwable{
        return execute(callback, null);
    }

    /**
     * 委托执行，支持降级和回滚，等待特定时间段，如果超时则失败
     * @param callback 获取锁成功执行的函数
     * @param faultCallback 获取所失败后降级参数，=null 时，获取失败将抛出 SharedLockTimeoutException
     * @param rollback 释放资源失败回退操作
     * @param time 等待时间
     * @param unit 时间单位
     * @param <T> 返回值类型
     * @return 返回值
     * @throws TimeoutLockException 获取锁失败
     * @throws Throwable 其他异常，包括业务异常等
     */
    <T> T execute(IDoCallback<T> callback, @Nullable IDoCallback<T> faultCallback, @Nullable IDoCallback<T> rollback, int time, TimeUnit unit) throws TimeoutLockException, Throwable;


    /**
     * 委托执行，支持降级和回滚，等待特定时间段，如果超时则失败
     * @param callback 获取锁成功执行的函数
     * @param faultCallback 获取所失败后降级参数，=null 时，获取失败将抛出 SharedLockTimeoutException
     * @param time 等待时间
     * @param unit 时间单位
     * @param <T> 返回值类型
     * @return 返回值
     * @throws TimeoutLockException 获取锁失败
     * @throws Throwable 其他异常，包括业务异常等
     */
    default <T> T execute(IDoCallback<T> callback, @Nullable IDoCallback<T> faultCallback, int time, TimeUnit unit) throws TimeoutLockException, Throwable {
        return execute(callback, faultCallback, null, time, unit);
    }

    /**
     * 委托执行，支持降级和回滚，等待特定时间段，如果超时则失败
     * @param callback 获取锁成功执行的函数
     * @param time 等待时间
     * @param unit 时间单位
     * @param <T> 返回值类型
     * @return 返回值
     * @throws TimeoutLockException 获取锁失败
     * @throws Throwable 其他异常，包括业务异常等
     */
    default <T> T execute(IDoCallback<T> callback, int time, TimeUnit unit) throws TimeoutLockException, Throwable {
        return execute(callback, time, unit);
    }
}
